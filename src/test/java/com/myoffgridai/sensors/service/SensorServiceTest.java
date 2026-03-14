package com.myoffgridai.sensors.service;

import com.myoffgridai.common.exception.DuplicateResourceException;
import com.myoffgridai.common.exception.EntityNotFoundException;
import com.myoffgridai.sensors.dto.CreateSensorRequest;
import com.myoffgridai.sensors.dto.SensorTestResult;
import com.myoffgridai.sensors.dto.UpdateThresholdsRequest;
import com.myoffgridai.sensors.model.DataFormat;
import com.myoffgridai.sensors.model.Sensor;
import com.myoffgridai.sensors.model.SensorReading;
import com.myoffgridai.sensors.model.SensorType;
import com.myoffgridai.sensors.repository.SensorReadingRepository;
import com.myoffgridai.sensors.repository.SensorRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class SensorServiceTest {

    @Mock private SensorRepository sensorRepository;
    @Mock private SensorReadingRepository readingRepository;
    @Mock private SensorPollingService pollingService;
    @Mock private SerialPortService serialPortService;

    private SensorService sensorService;
    private UUID userId;
    private UUID sensorId;
    private Sensor testSensor;

    @BeforeEach
    void setUp() {
        sensorService = new SensorService(
                sensorRepository, readingRepository, pollingService, serialPortService);

        userId = UUID.randomUUID();
        sensorId = UUID.randomUUID();
        testSensor = createSensor(sensorId, userId);
    }

    // ── registerSensor ──────────────────────────────────────────────────

    @Test
    void registerSensor_createsAndSaves() {
        CreateSensorRequest request = new CreateSensorRequest(
                "Temp Sensor", SensorType.TEMPERATURE, "/dev/ttyUSB0",
                9600, DataFormat.CSV_LINE, null, "°C", 30, null, null);

        when(sensorRepository.findByPortPath("/dev/ttyUSB0")).thenReturn(Optional.empty());
        when(sensorRepository.save(any(Sensor.class))).thenAnswer(i -> {
            Sensor s = i.getArgument(0);
            s.setId(UUID.randomUUID());
            return s;
        });

        Sensor result = sensorService.registerSensor(userId, request);

        assertNotNull(result.getId());
        assertEquals("Temp Sensor", result.getName());
        assertEquals(SensorType.TEMPERATURE, result.getType());
        verify(sensorRepository).save(any(Sensor.class));
    }

    @Test
    void registerSensor_duplicatePort_throws() {
        CreateSensorRequest request = new CreateSensorRequest(
                "Sensor", SensorType.TEMPERATURE, "/dev/ttyUSB0",
                9600, null, null, null, null, null, null);

        when(sensorRepository.findByPortPath("/dev/ttyUSB0"))
                .thenReturn(Optional.of(testSensor));

        assertThrows(DuplicateResourceException.class,
                () -> sensorService.registerSensor(userId, request));
    }

    @Test
    void registerSensor_usesDefaults() {
        CreateSensorRequest request = new CreateSensorRequest(
                "Sensor", SensorType.HUMIDITY, "/dev/ttyUSB1",
                null, null, null, null, null, null, null);

        when(sensorRepository.findByPortPath("/dev/ttyUSB1")).thenReturn(Optional.empty());
        when(sensorRepository.save(any(Sensor.class))).thenAnswer(i -> i.getArgument(0));

        Sensor result = sensorService.registerSensor(userId, request);

        assertEquals(9600, result.getBaudRate());
        assertEquals(DataFormat.CSV_LINE, result.getDataFormat());
        assertEquals(30, result.getPollIntervalSeconds());
    }

    // ── startSensor / stopSensor ────────────────────────────────────────

    @Test
    void startSensor_delegatesToPolling() {
        when(sensorRepository.findByIdAndUserId(sensorId, userId))
                .thenReturn(Optional.of(testSensor));
        when(sensorRepository.findById(sensorId)).thenReturn(Optional.of(testSensor));

        Sensor result = sensorService.startSensor(sensorId, userId);

        verify(pollingService).startPolling(testSensor);
        assertNotNull(result);
    }

    @Test
    void stopSensor_delegatesToPolling() {
        when(sensorRepository.findByIdAndUserId(sensorId, userId))
                .thenReturn(Optional.of(testSensor));
        when(sensorRepository.findById(sensorId)).thenReturn(Optional.of(testSensor));

        sensorService.stopSensor(sensorId, userId);

        verify(pollingService).stopPolling(sensorId);
    }

    // ── testSensor ──────────────────────────────────────────────────────

    @Test
    void testSensor_success_returnsSample() {
        when(serialPortService.testConnectionWithSample("/dev/ttyUSB0", 9600, 5000))
                .thenReturn("23.5,45.2");

        SensorTestResult result = sensorService.testSensor("/dev/ttyUSB0", 9600);

        assertTrue(result.success());
        assertEquals("23.5,45.2", result.sampleData());
    }

    @Test
    void testSensor_failure_returnsNoData() {
        when(serialPortService.testConnectionWithSample("/dev/ttyUSB0", 9600, 5000))
                .thenReturn(null);

        SensorTestResult result = sensorService.testSensor("/dev/ttyUSB0", 9600);

        assertFalse(result.success());
        assertNull(result.sampleData());
    }

    // ── getSensor / listSensors ─────────────────────────────────────────

    @Test
    void getSensor_found_returnsSensor() {
        when(sensorRepository.findByIdAndUserId(sensorId, userId))
                .thenReturn(Optional.of(testSensor));

        Sensor result = sensorService.getSensor(sensorId, userId);

        assertEquals(sensorId, result.getId());
    }

    @Test
    void getSensor_notFound_throws() {
        when(sensorRepository.findByIdAndUserId(sensorId, userId))
                .thenReturn(Optional.empty());

        assertThrows(EntityNotFoundException.class,
                () -> sensorService.getSensor(sensorId, userId));
    }

    @Test
    void listSensors_returnsUserSensors() {
        when(sensorRepository.findByUserIdOrderByNameAsc(userId))
                .thenReturn(List.of(testSensor));

        List<Sensor> result = sensorService.listSensors(userId);

        assertEquals(1, result.size());
    }

    // ── readings ────────────────────────────────────────────────────────

    @Test
    void getLatestReading_returnsReading() {
        when(sensorRepository.findByIdAndUserId(sensorId, userId))
                .thenReturn(Optional.of(testSensor));
        SensorReading reading = createReading();
        when(readingRepository.findTopBySensorIdOrderByRecordedAtDesc(sensorId))
                .thenReturn(Optional.of(reading));

        Optional<SensorReading> result = sensorService.getLatestReading(sensorId, userId);

        assertTrue(result.isPresent());
        assertEquals(25.5, result.get().getValue());
    }

    @Test
    void getReadingHistory_returnsPaginated() {
        when(sensorRepository.findByIdAndUserId(sensorId, userId))
                .thenReturn(Optional.of(testSensor));
        Page<SensorReading> page = new PageImpl<>(List.of(createReading()));
        when(readingRepository.findBySensorIdOrderByRecordedAtDesc(eq(sensorId), any(PageRequest.class)))
                .thenReturn(page);

        Page<SensorReading> result = sensorService.getReadingHistory(sensorId, userId, 24, 0, 20);

        assertEquals(1, result.getTotalElements());
    }

    // ── deleteSensor ────────────────────────────────────────────────────

    @Test
    void deleteSensor_stopsPollingAndDeletes() {
        when(sensorRepository.findByIdAndUserId(sensorId, userId))
                .thenReturn(Optional.of(testSensor));
        when(pollingService.isPolling(sensorId)).thenReturn(true);

        sensorService.deleteSensor(sensorId, userId);

        verify(pollingService).stopPolling(sensorId);
        verify(readingRepository).deleteBySensorId(sensorId);
        verify(sensorRepository).delete(testSensor);
    }

    @Test
    void deleteSensor_notPolling_skipsStop() {
        when(sensorRepository.findByIdAndUserId(sensorId, userId))
                .thenReturn(Optional.of(testSensor));
        when(pollingService.isPolling(sensorId)).thenReturn(false);

        sensorService.deleteSensor(sensorId, userId);

        verify(pollingService, never()).stopPolling(sensorId);
        verify(readingRepository).deleteBySensorId(sensorId);
        verify(sensorRepository).delete(testSensor);
    }

    // ── updateThresholds ────────────────────────────────────────────────

    @Test
    void updateThresholds_updatesAndSaves() {
        when(sensorRepository.findByIdAndUserId(sensorId, userId))
                .thenReturn(Optional.of(testSensor));
        when(sensorRepository.save(any(Sensor.class))).thenAnswer(i -> i.getArgument(0));

        UpdateThresholdsRequest request = new UpdateThresholdsRequest(10.0, 40.0);
        Sensor result = sensorService.updateThresholds(sensorId, userId, request);

        assertEquals(10.0, result.getLowThreshold());
        assertEquals(40.0, result.getHighThreshold());
        verify(sensorRepository).save(testSensor);
    }

    // ── listAvailablePorts / deleteAllForUser ────────────────────────────

    @Test
    void listAvailablePorts_delegates() {
        when(serialPortService.listAvailablePorts()).thenReturn(List.of("/dev/ttyUSB0"));

        List<String> result = sensorService.listAvailablePorts();

        assertEquals(1, result.size());
        assertEquals("/dev/ttyUSB0", result.get(0));
    }

    @Test
    void deleteAllForUser_stopsAndDeletes() {
        when(sensorRepository.findByUserIdOrderByNameAsc(userId))
                .thenReturn(List.of(testSensor));
        when(pollingService.isPolling(sensorId)).thenReturn(true);

        sensorService.deleteAllForUser(userId);

        verify(pollingService).stopPolling(sensorId);
        verify(readingRepository).deleteByUserId(userId);
        verify(sensorRepository).deleteByUserId(userId);
    }

    // ── helpers ──────────────────────────────────────────────────────────

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

    private SensorReading createReading() {
        SensorReading reading = new SensorReading();
        reading.setId(UUID.randomUUID());
        reading.setSensor(testSensor);
        reading.setValue(25.5);
        reading.setRawData("25.5");
        reading.setRecordedAt(Instant.now());
        return reading;
    }
}
