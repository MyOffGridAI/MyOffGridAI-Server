package com.myoffgridai.sensors.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fazecast.jSerialComm.SerialPort;
import com.myoffgridai.memory.service.MemoryService;
import com.myoffgridai.proactive.service.NotificationService;
import com.myoffgridai.sensors.model.DataFormat;
import com.myoffgridai.sensors.model.Sensor;
import com.myoffgridai.sensors.model.SensorType;
import com.myoffgridai.sensors.repository.SensorReadingRepository;
import com.myoffgridai.sensors.repository.SensorRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class SensorPollingServiceTest {

    @Mock private SerialPortService serialPortService;
    @Mock private SensorRepository sensorRepository;
    @Mock private SensorReadingRepository readingRepository;
    @Mock private SseEmitterRegistry sseEmitterRegistry;
    @Mock private MemoryService memoryService;
    @Mock private NotificationService notificationService;
    @Mock private SerialPort serialPort;

    private SensorPollingService pollingService;
    private ObjectMapper objectMapper;
    private Sensor testSensor;
    private UUID sensorId;
    private UUID userId;

    @BeforeEach
    void setUp() {
        objectMapper = new ObjectMapper();
        pollingService = new SensorPollingService(
                serialPortService, sensorRepository, readingRepository,
                sseEmitterRegistry, memoryService, notificationService, objectMapper);

        sensorId = UUID.randomUUID();
        userId = UUID.randomUUID();
        testSensor = createSensor(sensorId, userId);
    }

    @Test
    void startPolling_opensPortAndSchedules() {
        when(serialPort.isOpen()).thenReturn(true);
        when(serialPortService.openPort(testSensor.getPortPath(), testSensor.getBaudRate()))
                .thenReturn(serialPort);
        when(sensorRepository.save(any(Sensor.class))).thenAnswer(i -> i.getArgument(0));
        when(serialPortService.readLine(serialPort)).thenReturn(Optional.empty());

        pollingService.startPolling(testSensor);

        verify(serialPortService).openPort("/dev/ttyUSB0", 9600);
        assertTrue(pollingService.isPolling(sensorId));
        verify(sensorRepository).save(testSensor);
        assertTrue(testSensor.getIsActive());
    }

    @Test
    void startPolling_alreadyPolling_skips() {
        when(serialPort.isOpen()).thenReturn(true);
        when(serialPortService.openPort(anyString(), anyInt())).thenReturn(serialPort);
        when(sensorRepository.save(any(Sensor.class))).thenAnswer(i -> i.getArgument(0));
        when(serialPortService.readLine(serialPort)).thenReturn(Optional.empty());

        pollingService.startPolling(testSensor);
        pollingService.startPolling(testSensor);

        // openPort called only once
        verify(serialPortService, times(1)).openPort(anyString(), anyInt());
    }

    @Test
    void stopPolling_cancelsAndClosesPort() {
        when(serialPort.isOpen()).thenReturn(true);
        when(serialPortService.openPort(anyString(), anyInt())).thenReturn(serialPort);
        when(sensorRepository.save(any(Sensor.class))).thenAnswer(i -> i.getArgument(0));
        when(sensorRepository.findById(sensorId)).thenReturn(Optional.of(testSensor));
        when(serialPortService.readLine(serialPort)).thenReturn(Optional.empty());

        pollingService.startPolling(testSensor);
        assertTrue(pollingService.isPolling(sensorId));

        pollingService.stopPolling(sensorId);

        assertFalse(pollingService.isPolling(sensorId));
        verify(serialPortService, atLeastOnce()).closePort(serialPort);
        verify(sseEmitterRegistry, atLeastOnce()).remove(sensorId);
    }

    @Test
    void stopPolling_unknownSensor_noException() {
        assertDoesNotThrow(() -> pollingService.stopPolling(UUID.randomUUID()));
    }

    @Test
    void isPolling_notStarted_returnsFalse() {
        assertFalse(pollingService.isPolling(UUID.randomUUID()));
    }

    @Test
    void stopAllPolling_stopsAllActive() {
        UUID sensorId2 = UUID.randomUUID();
        Sensor sensor2 = createSensor(sensorId2, userId);
        sensor2.setPortPath("/dev/ttyUSB1");

        SerialPort port2 = mock(SerialPort.class);
        when(serialPort.isOpen()).thenReturn(true);
        when(port2.isOpen()).thenReturn(true);
        when(serialPortService.openPort("/dev/ttyUSB0", 9600)).thenReturn(serialPort);
        when(serialPortService.openPort("/dev/ttyUSB1", 9600)).thenReturn(port2);
        when(sensorRepository.save(any(Sensor.class))).thenAnswer(i -> i.getArgument(0));
        when(sensorRepository.findById(any(UUID.class))).thenReturn(Optional.of(testSensor));
        when(serialPortService.readLine(any(SerialPort.class))).thenReturn(Optional.empty());

        pollingService.startPolling(testSensor);
        pollingService.startPolling(sensor2);

        pollingService.stopAllPolling();

        assertFalse(pollingService.isPolling(sensorId));
        assertFalse(pollingService.isPolling(sensorId2));
    }

    private Sensor createSensor(UUID id, UUID userId) {
        Sensor sensor = new Sensor();
        sensor.setId(id);
        sensor.setUserId(userId);
        sensor.setName("Test Sensor");
        sensor.setType(SensorType.TEMPERATURE);
        sensor.setPortPath("/dev/ttyUSB0");
        sensor.setBaudRate(9600);
        sensor.setDataFormat(DataFormat.CSV_LINE);
        sensor.setPollIntervalSeconds(30);
        return sensor;
    }
}
