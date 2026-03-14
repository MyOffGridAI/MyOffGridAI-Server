package com.myoffgridai.integration;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.myoffgridai.ai.service.OllamaService;
import com.myoffgridai.auth.dto.LoginRequest;
import com.myoffgridai.auth.dto.RegisterRequest;
import com.myoffgridai.auth.model.Role;
import com.myoffgridai.knowledge.service.FileStorageService;
import com.myoffgridai.knowledge.service.OcrService;
import com.myoffgridai.memory.service.EmbeddingService;
import com.myoffgridai.privacy.dto.ExportRequest;
import com.myoffgridai.sensors.service.SerialPortService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Integration tests for data wipe and export operations.
 *
 * <p>Verifies that the {@code /api/privacy/wipe} endpoint enforces OWNER/ADMIN
 * authorization, that {@code /api/privacy/wipe/self} is available to all
 * authenticated users, that unauthenticated requests are rejected, and that
 * data export produces an encrypted binary response.</p>
 */
class DataWipeIntegrationTest extends BaseIntegrationTest {

    @Autowired private MockMvc mockMvc;
    @Autowired private ObjectMapper objectMapper;

    @MockBean private OllamaService ollamaService;
    @MockBean private EmbeddingService embeddingService;
    @MockBean private FileStorageService fileStorageService;
    @MockBean private OcrService ocrService;
    @MockBean private SerialPortService serialPortService;

    @BeforeEach
    void setUp() {
        when(ollamaService.isAvailable()).thenReturn(false);
    }

    /**
     * An OWNER user should be able to wipe data successfully,
     * receiving a response with success=true.
     */
    @Test
    void wipeData_ownerRole_succeeds() throws Exception {
        String accessToken = registerAndLogin(
                "wipeowner_" + System.nanoTime(), "pass", Role.ROLE_OWNER);

        mockMvc.perform(delete("/api/privacy/wipe")
                        .header("Authorization", "Bearer " + accessToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.success").value(true));
    }

    /**
     * A MEMBER user should be denied access to the wipe endpoint (403 Forbidden).
     */
    @Test
    void wipeData_memberRole_returns403() throws Exception {
        String accessToken = registerAndLogin(
                "wipemember_" + System.nanoTime(), "pass", Role.ROLE_MEMBER);

        mockMvc.perform(delete("/api/privacy/wipe")
                        .header("Authorization", "Bearer " + accessToken))
                .andExpect(status().isForbidden());
    }

    /**
     * A MEMBER user should be able to wipe their own data via the
     * /wipe/self endpoint, which has no role restriction.
     */
    @Test
    void wipeSelfData_memberRole_succeeds() throws Exception {
        String accessToken = registerAndLogin(
                "wipeselfmember_" + System.nanoTime(), "pass", Role.ROLE_MEMBER);

        mockMvc.perform(delete("/api/privacy/wipe/self")
                        .header("Authorization", "Bearer " + accessToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.success").value(true));
    }

    /**
     * An unauthenticated request to the wipe endpoint should return 401 Unauthorized.
     */
    @Test
    void wipeData_unauthenticated_returns401() throws Exception {
        mockMvc.perform(delete("/api/privacy/wipe"))
                .andExpect(status().isUnauthorized());
    }

    /**
     * The export endpoint should return an encrypted binary file
     * with content-type application/octet-stream when given a valid passphrase.
     */
    @Test
    void exportData_returnsEncryptedFile() throws Exception {
        String accessToken = registerAndLogin(
                "exportowner_" + System.nanoTime(), "pass", Role.ROLE_OWNER);

        ExportRequest exportRequest = new ExportRequest("securepassphrase123");

        mockMvc.perform(post("/api/privacy/export")
                        .header("Authorization", "Bearer " + accessToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(exportRequest)))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_OCTET_STREAM));
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
