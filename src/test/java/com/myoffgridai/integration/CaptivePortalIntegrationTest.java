package com.myoffgridai.integration;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.myoffgridai.system.dto.InitializeRequest;
import com.myoffgridai.system.repository.SystemConfigRepository;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Integration test for the captive portal setup flow.
 */
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
class CaptivePortalIntegrationTest extends BaseIntegrationTest {

    @Autowired private MockMvc mockMvc;
    @Autowired private ObjectMapper objectMapper;
    @Autowired private SystemConfigRepository systemConfigRepository;
    @Autowired private com.myoffgridai.auth.repository.UserRepository userRepository;

    @BeforeEach
    void cleanUp() {
        userRepository.deleteAll();
        systemConfigRepository.deleteAll();
    }

    @Test
    @Order(1)
    void systemStatus_cleanDb_returnsNotInitialized() throws Exception {
        mockMvc.perform(get("/api/system/status"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.initialized").value(false));
    }

    @Test
    @Order(2)
    void wifiScan_noAuth_returnsNetworkList() throws Exception {
        mockMvc.perform(get("/api/setup/wifi/scan"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data").isArray());
    }

    @Test
    @Order(3)
    void initialize_createsOwnerAndMarksInitialized() throws Exception {
        InitializeRequest request = new InitializeRequest(
                "Test Instance", "testowner", "Test Owner",
                "test@test.com", "pass");

        mockMvc.perform(post("/api/system/initialize")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.accessToken").exists());

        // Verify system is now initialized
        mockMvc.perform(get("/api/system/status"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.initialized").value(true))
                .andExpect(jsonPath("$.data.instanceName").value("Test Instance"));
    }

    @Test
    @Order(4)
    void setup_afterInitialization_redirects() throws Exception {
        // Initialize first
        InitializeRequest request = new InitializeRequest(
                "Test Instance", "testowner2", "Test Owner",
                "test2@test.com", "pass");
        mockMvc.perform(post("/api/system/initialize")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk());

        // Now /setup should redirect
        mockMvc.perform(get("/setup"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/"));
    }
}
