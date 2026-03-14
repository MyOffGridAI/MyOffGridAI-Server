package com.myoffgridai.sensors.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.myoffgridai.auth.model.Role;
import com.myoffgridai.auth.model.User;
import com.myoffgridai.auth.service.AuthService;
import com.myoffgridai.auth.service.JwtService;
import com.myoffgridai.config.CaptivePortalRedirectFilter;
import com.myoffgridai.config.JwtAuthFilter;
import com.myoffgridai.config.TestSecurityConfig;
import com.myoffgridai.sensors.dto.*;
import com.myoffgridai.sensors.model.DataFormat;
import com.myoffgridai.sensors.model.Sensor;
import com.myoffgridai.sensors.model.SensorReading;
import com.myoffgridai.sensors.model.SensorType;
import com.myoffgridai.sensors.service.SensorService;
import com.myoffgridai.sensors.service.SseEmitterRegistry;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.data.domain.PageImpl;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(SensorController.class)
@Import(TestSecurityConfig.class)
class SensorControllerTest {

    @Autowired private MockMvc mockMvc;
    @Autowired private ObjectMapper objectMapper;

    @MockBean private SensorService sensorService;
    @MockBean private SseEmitterRegistry sseEmitterRegistry;
    @MockBean private JwtService jwtService;
    @MockBean private JwtAuthFilter jwtAuthFilter;
    @MockBean private AuthService authService;
    @MockitoBean private CaptivePortalRedirectFilter captivePortalRedirectFilter;

    private User testUser;
    private UUID userId;
    private UUID sensorId;
    private Sensor testSensor;

    @BeforeEach
    void setUp() {
        userId = UUID.randomUUID();
        sensorId = UUID.randomUUID();

        testUser = new User();
        testUser.setId(userId);
        testUser.setUsername("testuser");
        testUser.setDisplayName("Test User");
        testUser.setRole(Role.ROLE_MEMBER);
        testUser.setPasswordHash("hash");

        testSensor = createSensor(sensorId, userId);
    }

    // ── List Sensors ────────────────────────────────────────────────────

    @Test
    void listSensors_returnsSensorList() throws Exception {
        when(sensorService.listSensors(userId)).thenReturn(List.of(testSensor));

        mockMvc.perform(get("/api/sensors").with(user(testUser)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data[0].name").value("Test Sensor"));
    }

    @Test
    void listSensors_unauthenticated_returns401() throws Exception {
        mockMvc.perform(get("/api/sensors"))
                .andExpect(status().isUnauthorized());
    }

    // ── Get Sensor ──────────────────────────────────────────────────────

    @Test
    void getSensor_returnsSensor() throws Exception {
        when(sensorService.getSensor(sensorId, userId)).thenReturn(testSensor);

        mockMvc.perform(get("/api/sensors/" + sensorId).with(user(testUser)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.name").value("Test Sensor"))
                .andExpect(jsonPath("$.data.type").value("TEMPERATURE"));
    }

    // ── Register Sensor ─────────────────────────────────────────────────

    @Test
    void registerSensor_returnsCreated() throws Exception {
        when(sensorService.registerSensor(eq(userId), any(CreateSensorRequest.class)))
                .thenReturn(testSensor);

        CreateSensorRequest request = new CreateSensorRequest(
                "New Sensor", SensorType.TEMPERATURE, "/dev/ttyUSB0",
                9600, DataFormat.CSV_LINE, null, "°C", 30, null, null);

        mockMvc.perform(post("/api/sensors")
                        .with(user(testUser))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.message").value("Sensor registered"));
    }

    @Test
    void registerSensor_missingName_returns400() throws Exception {
        CreateSensorRequest request = new CreateSensorRequest(
                "", SensorType.TEMPERATURE, "/dev/ttyUSB0",
                null, null, null, null, null, null, null);

        mockMvc.perform(post("/api/sensors")
                        .with(user(testUser))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest());
    }

    // ── Delete Sensor ───────────────────────────────────────────────────

    @Test
    void deleteSensor_returnsSuccess() throws Exception {
        doNothing().when(sensorService).deleteSensor(sensorId, userId);

        mockMvc.perform(delete("/api/sensors/" + sensorId).with(user(testUser)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("Sensor deleted"));

        verify(sensorService).deleteSensor(sensorId, userId);
    }

    // ── Start / Stop ────────────────────────────────────────────────────

    @Test
    void startSensor_returnsUpdated() throws Exception {
        testSensor.setIsActive(true);
        when(sensorService.startSensor(sensorId, userId)).thenReturn(testSensor);

        mockMvc.perform(post("/api/sensors/" + sensorId + "/start").with(user(testUser)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.isActive").value(true))
                .andExpect(jsonPath("$.message").value("Sensor started"));
    }

    @Test
    void stopSensor_returnsUpdated() throws Exception {
        testSensor.setIsActive(false);
        when(sensorService.stopSensor(sensorId, userId)).thenReturn(testSensor);

        mockMvc.perform(post("/api/sensors/" + sensorId + "/stop").with(user(testUser)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.isActive").value(false))
                .andExpect(jsonPath("$.message").value("Sensor stopped"));
    }

    // ── Latest Reading ──────────────────────────────────────────────────

    @Test
    void getLatestReading_returnsReading() throws Exception {
        SensorReading reading = createReading(testSensor, 23.5);
        when(sensorService.getLatestReading(sensorId, userId))
                .thenReturn(Optional.of(reading));

        mockMvc.perform(get("/api/sensors/" + sensorId + "/latest").with(user(testUser)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.value").value(23.5));
    }

    @Test
    void getLatestReading_noReading_returnsNullData() throws Exception {
        when(sensorService.getLatestReading(sensorId, userId))
                .thenReturn(Optional.empty());

        mockMvc.perform(get("/api/sensors/" + sensorId + "/latest").with(user(testUser)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true));
    }

    // ── Reading History ─────────────────────────────────────────────────

    @Test
    void getReadingHistory_returnsPaginated() throws Exception {
        SensorReading reading = createReading(testSensor, 25.0);
        when(sensorService.getReadingHistory(eq(sensorId), eq(userId), eq(24), eq(0), eq(20)))
                .thenReturn(new PageImpl<>(List.of(reading)));

        mockMvc.perform(get("/api/sensors/" + sensorId + "/history").with(user(testUser)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data[0].value").value(25.0))
                .andExpect(jsonPath("$.totalElements").value(1));
    }

    // ── Thresholds ──────────────────────────────────────────────────────

    @Test
    void updateThresholds_returnsUpdated() throws Exception {
        testSensor.setLowThreshold(5.0);
        testSensor.setHighThreshold(35.0);
        when(sensorService.updateThresholds(eq(sensorId), eq(userId), any(UpdateThresholdsRequest.class)))
                .thenReturn(testSensor);

        UpdateThresholdsRequest request = new UpdateThresholdsRequest(5.0, 35.0);

        mockMvc.perform(put("/api/sensors/" + sensorId + "/thresholds")
                        .with(user(testUser))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.lowThreshold").value(5.0))
                .andExpect(jsonPath("$.data.highThreshold").value(35.0));
    }

    // ── Connection Testing ──────────────────────────────────────────────

    @Test
    void testConnection_returnsResult() throws Exception {
        SensorTestResult result = new SensorTestResult(
                true, "/dev/ttyUSB0", 9600, "25.3", "Connection successful");
        when(sensorService.testSensor("/dev/ttyUSB0", 9600)).thenReturn(result);

        TestSensorRequest request = new TestSensorRequest("/dev/ttyUSB0", 9600);

        mockMvc.perform(post("/api/sensors/test")
                        .with(user(testUser))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.success").value(true))
                .andExpect(jsonPath("$.data.sampleData").value("25.3"));
    }

    // ── Available Ports ─────────────────────────────────────────────────

    @Test
    void listAvailablePorts_returnsPorts() throws Exception {
        when(sensorService.listAvailablePorts())
                .thenReturn(List.of("/dev/ttyUSB0", "/dev/ttyACM0"));

        mockMvc.perform(get("/api/sensors/ports").with(user(testUser)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data[0]").value("/dev/ttyUSB0"))
                .andExpect(jsonPath("$.data[1]").value("/dev/ttyACM0"));
    }

    // ── SSE Stream ──────────────────────────────────────────────────────

    @Test
    void streamSensor_returnsSseEmitter() throws Exception {
        when(sensorService.getSensor(sensorId, userId)).thenReturn(testSensor);

        mockMvc.perform(get("/api/sensors/" + sensorId + "/stream")
                        .with(user(testUser))
                        .accept(MediaType.TEXT_EVENT_STREAM_VALUE))
                .andExpect(status().isOk());

        verify(sseEmitterRegistry).register(eq(sensorId), any());
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
        sensor.setUnit("°C");
        sensor.setPollIntervalSeconds(30);
        sensor.setIsActive(false);
        sensor.setCreatedAt(Instant.now());
        sensor.setUpdatedAt(Instant.now());
        return sensor;
    }

    private SensorReading createReading(Sensor sensor, double value) {
        SensorReading reading = new SensorReading();
        reading.setId(UUID.randomUUID());
        reading.setSensor(sensor);
        reading.setValue(value);
        reading.setRawData(String.valueOf(value));
        reading.setRecordedAt(Instant.now());
        return reading;
    }
}
