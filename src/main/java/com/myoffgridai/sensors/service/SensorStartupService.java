package com.myoffgridai.sensors.service;

import com.myoffgridai.sensors.model.Sensor;
import com.myoffgridai.sensors.repository.SensorRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * Resumes polling for all sensors that were marked active when the server
 * last shut down. Runs automatically on application startup.
 */
@Service
public class SensorStartupService {

    private static final Logger log = LoggerFactory.getLogger(SensorStartupService.class);

    private final SensorRepository sensorRepository;
    private final SensorPollingService pollingService;

    /**
     * Constructs the sensor startup service.
     *
     * @param sensorRepository the sensor repository
     * @param pollingService   the sensor polling service
     */
    public SensorStartupService(SensorRepository sensorRepository,
                                SensorPollingService pollingService) {
        this.sensorRepository = sensorRepository;
        this.pollingService = pollingService;
    }

    /**
     * Resumes polling for all sensors flagged as active in the database.
     * Called automatically when the application is ready.
     */
    @EventListener(ApplicationReadyEvent.class)
    public void resumeActiveSensors() {
        List<Sensor> activeSensors = sensorRepository.findByIsActiveTrue();
        if (activeSensors.isEmpty()) {
            log.info("No active sensors to resume on startup");
            return;
        }

        log.info("Resuming {} active sensor(s) on startup", activeSensors.size());
        for (Sensor sensor : activeSensors) {
            try {
                pollingService.startPolling(sensor);
                log.info("Resumed sensor '{}' on {}", sensor.getName(), sensor.getPortPath());
            } catch (Exception e) {
                log.error("Failed to resume sensor '{}' on {}: {}",
                        sensor.getName(), sensor.getPortPath(), e.getMessage());
                sensor.setIsActive(false);
                sensorRepository.save(sensor);
            }
        }
    }
}
