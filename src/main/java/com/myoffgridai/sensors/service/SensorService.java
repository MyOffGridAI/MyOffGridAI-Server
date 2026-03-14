package com.myoffgridai.sensors.service;

import com.myoffgridai.common.exception.DuplicateResourceException;
import com.myoffgridai.common.exception.EntityNotFoundException;
import com.myoffgridai.config.AppConstants;
import com.myoffgridai.sensors.dto.CreateSensorRequest;
import com.myoffgridai.sensors.dto.SensorTestResult;
import com.myoffgridai.sensors.dto.UpdateThresholdsRequest;
import com.myoffgridai.sensors.model.DataFormat;
import com.myoffgridai.sensors.model.Sensor;
import com.myoffgridai.sensors.model.SensorReading;
import com.myoffgridai.sensors.repository.SensorReadingRepository;
import com.myoffgridai.sensors.repository.SensorRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * High-level service for sensor management. Coordinates between
 * the repository layer and the polling/serial-port services.
 */
@Service
public class SensorService {

    private static final Logger log = LoggerFactory.getLogger(SensorService.class);

    private final SensorRepository sensorRepository;
    private final SensorReadingRepository readingRepository;
    private final SensorPollingService pollingService;
    private final SerialPortService serialPortService;

    /**
     * Constructs the sensor service.
     *
     * @param sensorRepository  the sensor repository
     * @param readingRepository the sensor reading repository
     * @param pollingService    the sensor polling service
     * @param serialPortService the serial port service
     */
    public SensorService(SensorRepository sensorRepository,
                         SensorReadingRepository readingRepository,
                         SensorPollingService pollingService,
                         SerialPortService serialPortService) {
        this.sensorRepository = sensorRepository;
        this.readingRepository = readingRepository;
        this.pollingService = pollingService;
        this.serialPortService = serialPortService;
    }

    /**
     * Registers a new sensor for a user.
     *
     * @param userId  the user's ID
     * @param request the creation request
     * @return the created sensor
     * @throws DuplicateResourceException if the port path is already in use
     */
    public Sensor registerSensor(UUID userId, CreateSensorRequest request) {
        sensorRepository.findByPortPath(request.portPath()).ifPresent(existing -> {
            throw new DuplicateResourceException(
                    "Port path already in use: " + request.portPath());
        });

        Sensor sensor = new Sensor();
        sensor.setUserId(userId);
        sensor.setName(request.name());
        sensor.setType(request.type());
        sensor.setPortPath(request.portPath());
        sensor.setBaudRate(request.baudRate() != null ? request.baudRate() : 9600);
        sensor.setDataFormat(request.dataFormat() != null ? request.dataFormat() : DataFormat.CSV_LINE);
        sensor.setValueField(request.valueField());
        sensor.setUnit(request.unit());
        sensor.setPollIntervalSeconds(request.pollIntervalSeconds() != null
                ? request.pollIntervalSeconds()
                : AppConstants.DEFAULT_SENSOR_POLL_INTERVAL_SECONDS);
        sensor.setLowThreshold(request.lowThreshold());
        sensor.setHighThreshold(request.highThreshold());

        sensor = sensorRepository.save(sensor);
        log.info("Registered sensor '{}' on {} for user {}", sensor.getName(), sensor.getPortPath(), userId);
        return sensor;
    }

    /**
     * Starts polling for a sensor.
     *
     * @param sensorId the sensor ID
     * @param userId   the user's ID
     * @return the updated sensor
     */
    public Sensor startSensor(UUID sensorId, UUID userId) {
        Sensor sensor = findByIdAndUser(sensorId, userId);
        pollingService.startPolling(sensor);
        return sensorRepository.findById(sensorId).orElse(sensor);
    }

    /**
     * Stops polling for a sensor.
     *
     * @param sensorId the sensor ID
     * @param userId   the user's ID
     * @return the updated sensor
     */
    public Sensor stopSensor(UUID sensorId, UUID userId) {
        Sensor sensor = findByIdAndUser(sensorId, userId);
        pollingService.stopPolling(sensor.getId());
        return sensorRepository.findById(sensorId).orElse(sensor);
    }

    /**
     * Tests a serial port connection and returns sample data if available.
     *
     * @param portPath the port path to test
     * @param baudRate the baud rate
     * @return the test result with sample data
     */
    public SensorTestResult testSensor(String portPath, int baudRate) {
        String sample = serialPortService.testConnectionWithSample(
                portPath, baudRate, AppConstants.SENSOR_TEST_TIMEOUT_MS);
        if (sample != null) {
            return new SensorTestResult(true, portPath, baudRate, sample,
                    "Connection successful — received sample data");
        } else {
            return new SensorTestResult(false, portPath, baudRate, null,
                    "Connection failed — no data received within timeout");
        }
    }

    /**
     * Gets a sensor by ID, scoped to the user.
     *
     * @param sensorId the sensor ID
     * @param userId   the user's ID
     * @return the sensor
     * @throws EntityNotFoundException if not found
     */
    public Sensor getSensor(UUID sensorId, UUID userId) {
        return findByIdAndUser(sensorId, userId);
    }

    /**
     * Lists all sensors for a user, ordered by name.
     *
     * @param userId the user's ID
     * @return list of sensors
     */
    public List<Sensor> listSensors(UUID userId) {
        return sensorRepository.findByUserIdOrderByNameAsc(userId);
    }

    /**
     * Gets the latest reading for a sensor.
     *
     * @param sensorId the sensor ID
     * @param userId   the user's ID
     * @return the latest reading, or empty
     */
    public Optional<SensorReading> getLatestReading(UUID sensorId, UUID userId) {
        findByIdAndUser(sensorId, userId);
        return readingRepository.findTopBySensorIdOrderByRecordedAtDesc(sensorId);
    }

    /**
     * Gets paginated reading history for a sensor within the configured max hours.
     *
     * @param sensorId the sensor ID
     * @param userId   the user's ID
     * @param hours    the number of hours of history to return
     * @param page     the page number (0-based)
     * @param size     the page size
     * @return paginated sensor readings
     */
    public Page<SensorReading> getReadingHistory(UUID sensorId, UUID userId,
                                                  int hours, int page, int size) {
        findByIdAndUser(sensorId, userId);
        size = Math.min(size, AppConstants.MAX_PAGE_SIZE);
        return readingRepository.findBySensorIdOrderByRecordedAtDesc(sensorId,
                PageRequest.of(page, size));
    }

    /**
     * Deletes a sensor and all its readings.
     *
     * @param sensorId the sensor ID
     * @param userId   the user's ID
     */
    @Transactional
    public void deleteSensor(UUID sensorId, UUID userId) {
        Sensor sensor = findByIdAndUser(sensorId, userId);
        if (pollingService.isPolling(sensorId)) {
            pollingService.stopPolling(sensorId);
        }
        readingRepository.deleteBySensorId(sensorId);
        sensorRepository.delete(sensor);
        log.info("Deleted sensor '{}' and its readings for user {}", sensor.getName(), userId);
    }

    /**
     * Updates threshold values for a sensor.
     *
     * @param sensorId the sensor ID
     * @param userId   the user's ID
     * @param request  the threshold update request
     * @return the updated sensor
     */
    public Sensor updateThresholds(UUID sensorId, UUID userId, UpdateThresholdsRequest request) {
        Sensor sensor = findByIdAndUser(sensorId, userId);
        sensor.setLowThreshold(request.lowThreshold());
        sensor.setHighThreshold(request.highThreshold());
        sensor = sensorRepository.save(sensor);
        log.info("Updated thresholds for sensor '{}': low={}, high={}",
                sensor.getName(), request.lowThreshold(), request.highThreshold());
        return sensor;
    }

    /**
     * Lists all available serial ports on the device.
     *
     * @return list of port path strings
     */
    public List<String> listAvailablePorts() {
        return serialPortService.listAvailablePorts();
    }

    /**
     * Deletes all sensors and readings for a user.
     *
     * @param userId the user's ID
     */
    @Transactional
    public void deleteAllForUser(UUID userId) {
        List<Sensor> sensors = sensorRepository.findByUserIdOrderByNameAsc(userId);
        for (Sensor sensor : sensors) {
            if (pollingService.isPolling(sensor.getId())) {
                pollingService.stopPolling(sensor.getId());
            }
        }
        readingRepository.deleteByUserId(userId);
        sensorRepository.deleteByUserId(userId);
        log.info("Deleted all sensors and readings for user {}", userId);
    }

    private Sensor findByIdAndUser(UUID sensorId, UUID userId) {
        return sensorRepository.findByIdAndUserId(sensorId, userId)
                .orElseThrow(() -> new EntityNotFoundException("Sensor not found: " + sensorId));
    }
}
