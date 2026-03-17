package com.myoffgridai.integration;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.myoffgridai.ai.service.OllamaService;
import com.myoffgridai.auth.dto.LoginRequest;
import com.myoffgridai.auth.dto.RegisterRequest;
import com.myoffgridai.auth.model.Role;
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
 * Integration tests for fortress mode operations.
 *
 * <p>Verifies that fortress enable/disable is restricted to OWNER/ADMIN roles,
 * that MEMBER users are rejected with 403, and that the fortress status and
 * sovereignty report endpoints return the expected data structures.</p>
 */
class FortressIntegrationTest extends BaseIntegrationTest {

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
     * An OWNER user should be able to retrieve the fortress status,
     * which defaults to enabled=false on a fresh system.
     */
    @Test
    void getFortressStatus_returnsStatus() throws Exception {
        String accessToken = registerAndLogin(
                "fortressowner_" + System.nanoTime(), "pass", Role.ROLE_OWNER);

        mockMvc.perform(get("/api/privacy/fortress/status")
                        .header("Authorization", "Bearer " + accessToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.enabled").value(false));
    }

    /**
     * An OWNER user should be able to enable fortress mode.
     */
    @Test
    void enableFortress_ownerRole_succeeds() throws Exception {
        String accessToken = registerAndLogin(
                "fortenable_" + System.nanoTime(), "pass", Role.ROLE_OWNER);

        mockMvc.perform(post("/api/privacy/fortress/enable")
                        .header("Authorization", "Bearer " + accessToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true));
    }

    /**
     * After enabling fortress mode, an OWNER should be able to disable it.
     */
    @Test
    void disableFortress_afterEnable_succeeds() throws Exception {
        String accessToken = registerAndLogin(
                "fortdisable_" + System.nanoTime(), "pass", Role.ROLE_OWNER);

        // Enable first
        mockMvc.perform(post("/api/privacy/fortress/enable")
                        .header("Authorization", "Bearer " + accessToken))
                .andExpect(status().isOk());

        // Then disable
        mockMvc.perform(post("/api/privacy/fortress/disable")
                        .header("Authorization", "Bearer " + accessToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true));
    }

    /**
     * A MEMBER user should be denied access to enable fortress mode (403 Forbidden).
     */
    @Test
    void enableFortress_memberRole_returns403() throws Exception {
        String accessToken = registerAndLogin(
                "fortmember_" + System.nanoTime(), "pass", Role.ROLE_MEMBER);

        mockMvc.perform(post("/api/privacy/fortress/enable")
                        .header("Authorization", "Bearer " + accessToken))
                .andExpect(status().isForbidden());
    }

    /**
     * An OWNER user should be able to retrieve the sovereignty report
     * containing fortress status, data inventory, and audit summary fields.
     */
    @Test
    void getSovereigntyReport_returnsReport() throws Exception {
        String accessToken = registerAndLogin(
                "fortsovreport_" + System.nanoTime(), "pass", Role.ROLE_OWNER);

        mockMvc.perform(get("/api/privacy/sovereignty-report")
                        .header("Authorization", "Bearer " + accessToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.generatedAt").exists())
                .andExpect(jsonPath("$.data.fortressStatus").exists())
                .andExpect(jsonPath("$.data.encryptionStatus").exists())
                .andExpect(jsonPath("$.data.telemetryStatus").exists());
    }

    // ── Helpers ──────────────────────────────────────────────────────────

    private String registerAndLogin(String username, String password, Role role) throws Exception {
        RegisterRequest registerRequest = new RegisterRequest(
                username, null, username, password, role);
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
