package com.myoffgridai.integration;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.myoffgridai.ai.service.OllamaService;
import com.myoffgridai.knowledge.service.FileStorageService;
import com.myoffgridai.knowledge.service.OcrService;
import com.myoffgridai.memory.service.EmbeddingService;
import com.myoffgridai.sensors.service.SerialPortService;
import com.myoffgridai.system.dto.InitializeRequest;
import com.myoffgridai.system.repository.SystemConfigRepository;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.MethodOrderer;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.junit.jupiter.api.TestMethodOrder;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.http.MediaType;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.web.servlet.MockMvc;

import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Integration tests for the system initialization flow.
 *
 * <p>Tests the {@code /api/system/status} and {@code /api/system/initialize}
 * endpoints, verifying the first-boot setup sequence including owner account
 * creation and re-initialization rejection.</p>
 *
 * <p>Uses ordered test execution because the initialization endpoint mutates
 * shared system state (SystemConfig). Each test method depends on the state
 * left by the preceding method.</p>
 */
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_CLASS)
class SystemInitializationIntegrationTest extends BaseIntegrationTest {

    @Autowired private MockMvc mockMvc;
    @Autowired private ObjectMapper objectMapper;
    @Autowired private SystemConfigRepository systemConfigRepository;
    @Autowired private com.myoffgridai.auth.repository.UserRepository userRepository;

    @MockitoBean private OllamaService ollamaService;
    @MockitoBean private EmbeddingService embeddingService;
    @MockitoBean private FileStorageService fileStorageService;
    @MockitoBean private OcrService ocrService;
    @MockitoBean private SerialPortService serialPortService;

    private static final String INSTANCE_NAME = "TestGrid_" + System.nanoTime();
    private static final String OWNER_USERNAME = "owner_" + System.nanoTime();
    private static final String OWNER_PASSWORD = "pass";

    @BeforeAll
    void cleanDatabase() {
        userRepository.deleteAll();
        systemConfigRepository.deleteAll();
    }

    @BeforeEach
    void setUp() {
        when(ollamaService.isAvailable()).thenReturn(false);
    }

    /**
     * Before initialization, the system status should report initialized=false
     * and include the serverVersion field.
     */
    @Test
    @Order(1)
    void getStatus_returnsSystemStatus() throws Exception {
        mockMvc.perform(get("/api/system/status"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.initialized").value(false))
                .andExpect(jsonPath("$.data.serverVersion").exists());
    }

    /**
     * The initialize endpoint should create the owner account and return
     * an accessToken in the response.
     */
    @Test
    @Order(2)
    void initialize_createsOwnerAndInitializes() throws Exception {
        InitializeRequest request = new InitializeRequest(
                INSTANCE_NAME,
                OWNER_USERNAME,
                "Test Owner",
                OWNER_USERNAME + "@test.com",
                OWNER_PASSWORD
        );

        mockMvc.perform(post("/api/system/initialize")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.accessToken").exists());
    }

    /**
     * A second initialization attempt should be rejected with 409 Conflict.
     */
    @Test
    @Order(3)
    void initialize_secondTime_returns409() throws Exception {
        InitializeRequest request = new InitializeRequest(
                "AnotherGrid",
                "anotherowner_" + System.nanoTime(),
                "Another Owner",
                "another@test.com",
                "pass"
        );

        mockMvc.perform(post("/api/system/initialize")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isConflict());
    }

    /**
     * After initialization, the status endpoint should report initialized=true
     * and return the configured instance name.
     */
    @Test
    @Order(4)
    void getStatus_afterInit_showsInitialized() throws Exception {
        mockMvc.perform(get("/api/system/status"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.initialized").value(true))
                .andExpect(jsonPath("$.data.instanceName").value(INSTANCE_NAME));
    }
}
