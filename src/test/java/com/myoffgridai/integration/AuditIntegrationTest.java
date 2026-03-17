package com.myoffgridai.integration;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.myoffgridai.ai.service.OllamaService;
import com.myoffgridai.auth.dto.LoginRequest;
import com.myoffgridai.auth.dto.RegisterRequest;
import com.myoffgridai.knowledge.service.FileStorageService;
import com.myoffgridai.knowledge.service.OcrService;
import com.myoffgridai.memory.service.EmbeddingService;
import com.myoffgridai.sensors.service.SerialPortService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Integration tests for the audit logging system.
 *
 * <p>Verifies that the {@code /api/privacy/audit-logs} endpoint correctly
 * records and returns audit entries for authenticated requests, respects
 * authentication requirements, and supports pagination.</p>
 */
class AuditIntegrationTest extends BaseIntegrationTest {

    @Autowired private MockMvc mockMvc;
    @Autowired private ObjectMapper objectMapper;

    @MockitoBean private OllamaService ollamaService;
    @MockitoBean private EmbeddingService embeddingService;
    @MockitoBean private FileStorageService fileStorageService;
    @MockitoBean private OcrService ocrService;
    @MockitoBean private SerialPortService serialPortService;

    @BeforeEach
    void setUp() {
        when(ollamaService.isAvailable()).thenReturn(false);
    }

    /**
     * Accessing audit logs without authentication should return 401 Unauthorized.
     */
    @Test
    void auditLogs_unauthenticated_returns401() throws Exception {
        mockMvc.perform(get("/api/privacy/audit-logs"))
                .andExpect(status().isUnauthorized());
    }

    /**
     * After registering and logging in (which generates audit events),
     * the audit logs endpoint should return entries.
     */
    @Test
    void auditLogs_authenticated_returnsLogs() throws Exception {
        String accessToken = registerAndLogin("audituser_" + System.nanoTime(), "pass");

        // Make a few requests to generate audit entries
        mockMvc.perform(get("/api/system/status"))
                .andExpect(status().isOk());

        mockMvc.perform(get("/api/privacy/audit-logs")
                        .header("Authorization", "Bearer " + accessToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data").isArray());
    }

    /**
     * Audit log entries should contain request metadata including
     * httpMethod, requestPath, and outcome.
     */
    @Test
    void auditLogs_capturesRequestInfo() throws Exception {
        String accessToken = registerAndLogin("auditcapture_" + System.nanoTime(), "pass");

        // Make a request that will be audited
        mockMvc.perform(get("/api/privacy/audit-logs")
                        .header("Authorization", "Bearer " + accessToken))
                .andExpect(status().isOk());

        // Retrieve audit logs and verify entry fields
        mockMvc.perform(get("/api/privacy/audit-logs")
                        .header("Authorization", "Bearer " + accessToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data").isArray())
                .andExpect(jsonPath("$.data[0].httpMethod").exists())
                .andExpect(jsonPath("$.data[0].requestPath").exists())
                .andExpect(jsonPath("$.data[0].outcome").exists());
    }

    /**
     * The audit logs endpoint should support pagination parameters.
     */
    @Test
    void auditLogs_pagination() throws Exception {
        String accessToken = registerAndLogin("auditpage_" + System.nanoTime(), "pass");

        mockMvc.perform(get("/api/privacy/audit-logs?page=0&size=5")
                        .header("Authorization", "Bearer " + accessToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true));
    }

    // ── Helpers ──────────────────────────────────────────────────────────

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
