package com.myoffgridai.sensors.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fazecast.jSerialComm.SerialPort;
import com.myoffgridai.common.exception.SensorConnectionException;
import com.myoffgridai.config.AppConstants;
import com.myoffgridai.memory.model.MemoryImportance;
import com.myoffgridai.memory.service.MemoryService;
import com.myoffgridai.proactive.model.NotificationType;
import com.myoffgridai.proactive.service.NotificationService;
import com.myoffgridai.sensors.model.DataFormat;
import com.myoffgridai.sensors.model.Sensor;
import com.myoffgridai.sensors.model.SensorReading;
import com.myoffgridai.sensors.repository.SensorReadingRepository;
import com.myoffgridai.sensors.repository.SensorRepository;
import jakarta.annotation.PreDestroy;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.TaskScheduler;
import org.springframework.scheduling.concurrent.ThreadPoolTaskScheduler;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.time.Instant;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ScheduledFuture;

/**
 * Manages active polling loops for registered sensors. Each active sensor
 * runs in its own scheduled thread with configurable poll intervals.
 */
@Service
public class SensorPollingService {

    private static final Logger log = LoggerFactory.getLogger(SensorPollingService.class);

    private final SerialPortService serialPortService;
    private final SensorRepository sensorRepository;
    private final SensorReadingRepository readingRepository;
    private final SseEmitterRegistry sseEmitterRegistry;
    private final MemoryService memoryService;
    private final NotificationService notificationService;
    private final ObjectMapper objectMapper;
    private final TaskScheduler taskScheduler;

    private final ConcurrentHashMap<UUID, ScheduledFuture<?>> activeFutures = new ConcurrentHashMap<>();
    private final ConcurrentHashMap<UUID, SerialPort> activePorts = new ConcurrentHashMap<>();
    private final ConcurrentHashMap<UUID, Integer> failureCounters = new ConcurrentHashMap<>();

    /**
     * Constructs the sensor polling service.
     *
     * @param serialPortService   the serial port wrapper service
     * @param sensorRepository    the sensor repository
     * @param readingRepository   the sensor reading repository
     * @param sseEmitterRegistry  the SSE emitter registry
     * @param memoryService       the memory service for threshold alerts
     * @param notificationService the notification service for threshold alerts
     * @param objectMapper        the JSON object mapper
     */
    public SensorPollingService(SerialPortService serialPortService,
                                SensorRepository sensorRepository,
                                SensorReadingRepository readingRepository,
                                SseEmitterRegistry sseEmitterRegistry,
                                MemoryService memoryService,
                                NotificationService notificationService,
                                ObjectMapper objectMapper) {
        this.serialPortService = serialPortService;
        this.sensorRepository = sensorRepository;
        this.readingRepository = readingRepository;
        this.sseEmitterRegistry = sseEmitterRegistry;
        this.memoryService = memoryService;
        this.notificationService = notificationService;
        this.objectMapper = objectMapper;

        ThreadPoolTaskScheduler scheduler = new ThreadPoolTaskScheduler();
        scheduler.setPoolSize(10);
        scheduler.setThreadNamePrefix("sensor-poll-");
        scheduler.initialize();
        this.taskScheduler = scheduler;
    }

    /**
     * Starts polling for a sensor. Opens the serial port and schedules
     * periodic reads at the sensor's configured poll interval.
     *
     * @param sensor the sensor to start polling
     * @throws SensorConnectionException if the serial port cannot be opened
     */
    public void startPolling(Sensor sensor) {
        if (activeFutures.containsKey(sensor.getId())) {
            log.warn("Sensor {} is already being polled", sensor.getName());
            return;
        }

        SerialPort port = serialPortService.openPort(sensor.getPortPath(), sensor.getBaudRate());
        activePorts.put(sensor.getId(), port);
        failureCounters.put(sensor.getId(), 0);

        ScheduledFuture<?> future = taskScheduler.scheduleAtFixedRate(
                () -> pollOnce(sensor),
                Duration.ofSeconds(sensor.getPollIntervalSeconds()));
        activeFutures.put(sensor.getId(), future);

        sensor.setIsActive(true);
        sensorRepository.save(sensor);
        log.info("Started polling sensor '{}' on {} every {}s",
                sensor.getName(), sensor.getPortPath(), sensor.getPollIntervalSeconds());
    }

    /**
     * Stops polling for a sensor. Cancels the scheduled task and closes the port.
     *
     * @param sensorId the sensor ID
     */
    public void stopPolling(UUID sensorId) {
        ScheduledFuture<?> future = activeFutures.remove(sensorId);
        if (future != null) {
            future.cancel(false);
        }

        SerialPort port = activePorts.remove(sensorId);
        if (port != null) {
            serialPortService.closePort(port);
        }
        failureCounters.remove(sensorId);
        sseEmitterRegistry.remove(sensorId);

        sensorRepository.findById(sensorId).ifPresent(sensor -> {
            sensor.setIsActive(false);
            sensorRepository.save(sensor);
            log.info("Stopped polling sensor '{}'", sensor.getName());
        });
    }

    /**
     * Stops polling for all active sensors. Called on application shutdown.
     */
    @PreDestroy
    public void stopAllPolling() {
        log.info("Stopping all sensor polling ({} active)", activeFutures.size());
        for (UUID sensorId : activeFutures.keySet()) {
            stopPolling(sensorId);
        }
    }

    /**
     * Checks if a sensor is currently being polled.
     *
     * @param sensorId the sensor ID
     * @return true if the sensor is actively polled
     */
    public boolean isPolling(UUID sensorId) {
        return activeFutures.containsKey(sensorId);
    }

    private void pollOnce(Sensor sensor) {
        try {
            SerialPort port = activePorts.get(sensor.getId());
            if (port == null || !port.isOpen()) {
                log.warn("Port closed for sensor '{}', stopping polling", sensor.getName());
                stopPolling(sensor.getId());
                return;
            }

            Optional<String> rawLine = serialPortService.readLine(port);
            if (rawLine.isEmpty()) {
                log.debug("Empty read from sensor '{}'", sensor.getName());
                return;
            }

            Optional<Double> parsedValue = parseLine(sensor, rawLine.get());
            if (parsedValue.isEmpty()) {
                int failures = failureCounters.merge(sensor.getId(), 1, Integer::sum);
                log.warn("Parse failure #{} for sensor '{}': raw='{}'",
                        failures, sensor.getName(), rawLine.get());
                if (failures >= AppConstants.SENSOR_CONSECUTIVE_FAILURE_LIMIT) {
                    log.error("Sensor '{}' exceeded {} consecutive parse failures, stopping",
                            sensor.getName(), AppConstants.SENSOR_CONSECUTIVE_FAILURE_LIMIT);
                    stopPolling(sensor.getId());
                }
                return;
            }

            // Reset failure counter on success
            failureCounters.put(sensor.getId(), 0);

            double value = parsedValue.get();
            Instant now = Instant.now();

            SensorReading reading = new SensorReading();
            reading.setSensor(sensor);
            reading.setValue(value);
            reading.setRawData(rawLine.get());
            reading.setRecordedAt(now);
            readingRepository.save(reading);

            sseEmitterRegistry.broadcast(sensor.getId(), value, now);
            checkThresholds(sensor, value);

            log.debug("Sensor '{}' reading: {} {}", sensor.getName(), value,
                    sensor.getUnit() != null ? sensor.getUnit() : "");

        } catch (Exception e) {
            log.error("Error polling sensor '{}': {}", sensor.getName(), e.getMessage());
        }
    }

    private Optional<Double> parseLine(Sensor sensor, String rawLine) {
        try {
            if (sensor.getDataFormat() == DataFormat.CSV_LINE) {
                String token = rawLine.contains(",") ? rawLine.split(",")[0] : rawLine;
                return Optional.of(Double.parseDouble(token.trim()));
            } else if (sensor.getDataFormat() == DataFormat.JSON_LINE) {
                JsonNode node = objectMapper.readTree(rawLine);
                String field = sensor.getValueField() != null ? sensor.getValueField() : "value";
                JsonNode valueNode = node.get(field);
                if (valueNode != null && valueNode.isNumber()) {
                    return Optional.of(valueNode.asDouble());
                }
                return Optional.empty();
            }
            return Optional.empty();
        } catch (Exception e) {
            return Optional.empty();
        }
    }

    private void checkThresholds(Sensor sensor, double value) {
        String unit = sensor.getUnit() != null ? sensor.getUnit() : "";

        if (sensor.getLowThreshold() != null && value <= sensor.getLowThreshold()) {
            log.warn("Threshold alert: {} value {} below low threshold {}",
                    sensor.getName(), value, sensor.getLowThreshold());
            String content = String.format(
                    "Sensor '%s' reading %.2f%s dropped below low threshold %.2f%s on %s",
                    sensor.getName(), value, unit, sensor.getLowThreshold(), unit, Instant.now());
            memoryService.createMemory(sensor.getUserId(), content,
                    MemoryImportance.HIGH, "sensor-alert", null);
            notificationService.createNotification(
                    sensor.getUserId(),
                    "Sensor Alert: " + sensor.getName(),
                    String.format("Reading %.2f%s dropped below low threshold %.2f%s",
                            value, unit, sensor.getLowThreshold(), unit),
                    NotificationType.SENSOR_ALERT,
                    "{\"sensorId\":\"" + sensor.getId() + "\",\"value\":" + value + ",\"threshold\":\"low\"}");
        }

        if (sensor.getHighThreshold() != null && value >= sensor.getHighThreshold()) {
            log.warn("Threshold alert: {} value {} above high threshold {}",
                    sensor.getName(), value, sensor.getHighThreshold());
            String content = String.format(
                    "Sensor '%s' reading %.2f%s exceeded high threshold %.2f%s on %s",
                    sensor.getName(), value, unit, sensor.getHighThreshold(), unit, Instant.now());
            memoryService.createMemory(sensor.getUserId(), content,
                    MemoryImportance.HIGH, "sensor-alert", null);
            notificationService.createNotification(
                    sensor.getUserId(),
                    "Sensor Alert: " + sensor.getName(),
                    String.format("Reading %.2f%s exceeded high threshold %.2f%s",
                            value, unit, sensor.getHighThreshold(), unit),
                    NotificationType.SENSOR_ALERT,
                    "{\"sensorId\":\"" + sensor.getId() + "\",\"value\":" + value + ",\"threshold\":\"high\"}");
        }
    }
}
