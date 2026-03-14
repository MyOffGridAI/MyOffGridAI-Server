package com.myoffgridai.integration;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.myoffgridai.ai.service.OllamaService;
import com.myoffgridai.auth.dto.LoginRequest;
import com.myoffgridai.auth.dto.RegisterRequest;
import com.myoffgridai.knowledge.service.FileStorageService;
import com.myoffgridai.knowledge.service.OcrService;
import com.myoffgridai.memory.service.EmbeddingService;
import com.myoffgridai.sensors.dto.CreateSensorRequest;
import com.myoffgridai.sensors.dto.UpdateThresholdsRequest;
import com.myoffgridai.sensors.model.DataFormat;
import com.myoffgridai.sensors.model.SensorType;
import com.myoffgridai.sensors.service.SerialPortService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import java.util.UUID;

import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

class SensorIntegrationTest extends BaseIntegrationTest {

    @Autowired private MockMvc mockMvc;
    @Autowired private ObjectMapper objectMapper;

    @MockBean private OllamaService ollamaService;
    @MockBean private EmbeddingService embeddingService;
    @MockBean private FileStorageService fileStorageService;
    @MockBean private OcrService ocrService;
    @MockBean private SerialPortService serialPortService;

    private String accessToken;

    @BeforeEach
    void setUp() throws Exception {
        when(ollamaService.isAvailable()).thenReturn(false);
        accessToken = registerAndLogin("sensoruser_" + System.nanoTime(), "pass");
    }

    // ── Register Sensor ─────────────────────────────────────────────────

    @Test
    void registerSensor_returnsCreatedSensor() throws Exception {
        CreateSensorRequest request = new CreateSensorRequest(
                "Temp Sensor", SensorType.TEMPERATURE, "/dev/ttyUSB0",
                9600, DataFormat.CSV_LINE, null, "°C", 30, null, null);

        mockMvc.perform(post("/api/sensors")
                        .header("Authorization", "Bearer " + accessToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.name").value("Temp Sensor"))
                .andExpect(jsonPath("$.data.type").value("TEMPERATURE"))
                .andExpect(jsonPath("$.data.portPath").value("/dev/ttyUSB0"))
                .andExpect(jsonPath("$.data.isActive").value(false));
    }

    @Test
    void registerSensor_duplicatePort_returns409() throws Exception {
        CreateSensorRequest request = new CreateSensorRequest(
                "Sensor A", SensorType.TEMPERATURE, "/dev/ttyUSB_dup_" + System.nanoTime(),
                9600, null, null, null, null, null, null);

        mockMvc.perform(post("/api/sensors")
                        .header("Authorization", "Bearer " + accessToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk());

        // Same port path again
        CreateSensorRequest dup = new CreateSensorRequest(
                "Sensor B", SensorType.HUMIDITY, request.portPath(),
                9600, null, null, null, null, null, null);

        mockMvc.perform(post("/api/sensors")
                        .header("Authorization", "Bearer " + accessToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(dup)))
                .andExpect(status().isConflict());
    }

    // ── List Sensors ────────────────────────────────────────────────────

    @Test
    void listSensors_returnsUserSensors() throws Exception {
        registerSensor("Sensor List Test", "/dev/ttyList_" + System.nanoTime());

        mockMvc.perform(get("/api/sensors")
                        .header("Authorization", "Bearer " + accessToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data").isArray())
                .andExpect(jsonPath("$.data.length()").value(
                        org.hamcrest.Matchers.greaterThanOrEqualTo(1)));
    }

    // ── Get Sensor ──────────────────────────────────────────────────────

    @Test
    void getSensor_returnsSensor() throws Exception {
        String sensorId = registerSensor("Get Test", "/dev/ttyGet_" + System.nanoTime());

        mockMvc.perform(get("/api/sensors/" + sensorId)
                        .header("Authorization", "Bearer " + accessToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.name").value("Get Test"));
    }

    @Test
    void getSensor_notFound_returns404() throws Exception {
        mockMvc.perform(get("/api/sensors/" + UUID.randomUUID())
                        .header("Authorization", "Bearer " + accessToken))
                .andExpect(status().isNotFound());
    }

    // ── Update Thresholds ───────────────────────────────────────────────

    @Test
    void updateThresholds_updatesAndReturns() throws Exception {
        String sensorId = registerSensor("Threshold Test", "/dev/ttyThresh_" + System.nanoTime());

        UpdateThresholdsRequest request = new UpdateThresholdsRequest(5.0, 40.0);
        mockMvc.perform(put("/api/sensors/" + sensorId + "/thresholds")
                        .header("Authorization", "Bearer " + accessToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.lowThreshold").value(5.0))
                .andExpect(jsonPath("$.data.highThreshold").value(40.0));
    }

    // ── Delete Sensor ───────────────────────────────────────────────────

    @Test
    void deleteSensor_removesSuccessfully() throws Exception {
        String sensorId = registerSensor("Delete Test", "/dev/ttyDel_" + System.nanoTime());

        mockMvc.perform(delete("/api/sensors/" + sensorId)
                        .header("Authorization", "Bearer " + accessToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("Sensor deleted"));

        // Verify it's gone
        mockMvc.perform(get("/api/sensors/" + sensorId)
                        .header("Authorization", "Bearer " + accessToken))
                .andExpect(status().isNotFound());
    }

    // ── Latest Reading (empty) ──────────────────────────────────────────

    @Test
    void getLatestReading_noReadings_returnsNull() throws Exception {
        String sensorId = registerSensor("Reading Test", "/dev/ttyRead_" + System.nanoTime());

        mockMvc.perform(get("/api/sensors/" + sensorId + "/latest")
                        .header("Authorization", "Bearer " + accessToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true));
    }

    // ── Unauthenticated ─────────────────────────────────────────────────

    @Test
    void sensorEndpoints_unauthenticated_returns401() throws Exception {
        mockMvc.perform(get("/api/sensors"))
                .andExpect(status().isUnauthorized());

        mockMvc.perform(post("/api/sensors")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isUnauthorized());

        mockMvc.perform(get("/api/sensors/" + UUID.randomUUID()))
                .andExpect(status().isUnauthorized());
    }

    // ── Available Ports ─────────────────────────────────────────────────

    @Test
    void listAvailablePorts_returnsOk() throws Exception {
        mockMvc.perform(get("/api/sensors/ports")
                        .header("Authorization", "Bearer " + accessToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data").isArray());
    }

    // ── helpers ──────────────────────────────────────────────────────────

    private String registerSensor(String name, String portPath) throws Exception {
        CreateSensorRequest request = new CreateSensorRequest(
                name, SensorType.TEMPERATURE, portPath,
                9600, DataFormat.CSV_LINE, null, "°C", 30, null, null);

        MvcResult result = mockMvc.perform(post("/api/sensors")
                        .header("Authorization", "Bearer " + accessToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andReturn();

        return objectMapper.readTree(
                result.getResponse().getContentAsString())
                .path("data").path("id").asText();
    }

    private String registerAndLogin(String username, String password) throws Exception {
        RegisterRequest registerRequest = new RegisterRequest(
                username, null, username, password, null);
        mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(registerRequest)))
                .andExpect(status().isCreated());

        LoginRequest loginRequest = new LoginRequest(username, password);
        MvcResult loginResult = mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(loginRequest)))
                .andExpect(status().isOk())
                .andReturn();

        return objectMapper.readTree(
                loginResult.getResponse().getContentAsString())
                .path("data").path("accessToken").asText();
    }
}
