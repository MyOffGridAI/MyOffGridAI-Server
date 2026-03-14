package com.myoffgridai.sensors.service;

import com.myoffgridai.common.exception.SensorConnectionException;
import com.myoffgridai.sensors.model.DataFormat;
import com.myoffgridai.sensors.model.Sensor;
import com.myoffgridai.sensors.model.SensorType;
import com.myoffgridai.sensors.repository.SensorRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class SensorStartupServiceTest {

    @Mock private SensorRepository sensorRepository;
    @Mock private SensorPollingService pollingService;

    private SensorStartupService startupService;

    @BeforeEach
    void setUp() {
        startupService = new SensorStartupService(sensorRepository, pollingService);
    }

    @Test
    void resumeActiveSensors_noActive_doesNothing() {
        when(sensorRepository.findByIsActiveTrue()).thenReturn(List.of());

        startupService.resumeActiveSensors();

        verify(pollingService, never()).startPolling(any());
    }

    @Test
    void resumeActiveSensors_startsPollingForActive() {
        Sensor sensor1 = createSensor("/dev/ttyUSB0");
        Sensor sensor2 = createSensor("/dev/ttyUSB1");
        when(sensorRepository.findByIsActiveTrue()).thenReturn(List.of(sensor1, sensor2));

        startupService.resumeActiveSensors();

        verify(pollingService).startPolling(sensor1);
        verify(pollingService).startPolling(sensor2);
    }

    @Test
    void resumeActiveSensors_handleFailureGracefully() {
        Sensor sensor1 = createSensor("/dev/ttyUSB0");
        Sensor sensor2 = createSensor("/dev/ttyUSB1");
        when(sensorRepository.findByIsActiveTrue()).thenReturn(List.of(sensor1, sensor2));
        doThrow(new SensorConnectionException("Port not found"))
                .when(pollingService).startPolling(sensor1);
        when(sensorRepository.save(any(Sensor.class))).thenAnswer(i -> i.getArgument(0));

        startupService.resumeActiveSensors();

        // sensor1 failed — should be deactivated
        verify(sensorRepository).save(sensor1);
        // sensor2 should still start
        verify(pollingService).startPolling(sensor2);
    }

    @Test
    void resumeActiveSensors_setsInactiveFalseOnFailure() {
        Sensor sensor = createSensor("/dev/ttyUSB0");
        sensor.setIsActive(true);
        when(sensorRepository.findByIsActiveTrue()).thenReturn(List.of(sensor));
        doThrow(new SensorConnectionException("Cannot open"))
                .when(pollingService).startPolling(sensor);
        when(sensorRepository.save(any(Sensor.class))).thenAnswer(i -> i.getArgument(0));

        startupService.resumeActiveSensors();

        assertFalse(sensor.getIsActive());
        verify(sensorRepository).save(sensor);
    }

    private Sensor createSensor(String portPath) {
        Sensor sensor = new Sensor();
        sensor.setId(UUID.randomUUID());
        sensor.setUserId(UUID.randomUUID());
        sensor.setName("Sensor on " + portPath);
        sensor.setType(SensorType.TEMPERATURE);
        sensor.setPortPath(portPath);
        sensor.setBaudRate(9600);
        sensor.setDataFormat(DataFormat.CSV_LINE);
        sensor.setPollIntervalSeconds(30);
        sensor.setIsActive(true);
        return sensor;
    }

    // Using static import for assertFalse
    private void assertFalse(boolean value) {
        org.junit.jupiter.api.Assertions.assertFalse(value);
    }
}
