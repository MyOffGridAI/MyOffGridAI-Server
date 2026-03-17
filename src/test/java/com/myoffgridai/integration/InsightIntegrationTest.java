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

import java.util.UUID;

import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

class InsightIntegrationTest extends BaseIntegrationTest {

    @Autowired private MockMvc mockMvc;
    @Autowired private ObjectMapper objectMapper;

    @MockitoBean private OllamaService ollamaService;
    @MockitoBean private EmbeddingService embeddingService;
    @MockitoBean private FileStorageService fileStorageService;
    @MockitoBean private OcrService ocrService;
    @MockitoBean private SerialPortService serialPortService;

    private String accessToken;

    @BeforeEach
    void setUp() throws Exception {
        when(ollamaService.isAvailable()).thenReturn(false);
        accessToken = registerAndLogin("insightuser_" + System.nanoTime(), "pass");
    }

    // ── Get Insights ─────────────────────────────────────────────────────

    @Test
    void getInsights_emptyList() throws Exception {
        mockMvc.perform(get("/api/insights")
                        .header("Authorization", "Bearer " + accessToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data").isArray())
                .andExpect(jsonPath("$.data.length()").value(0));
    }

    @Test
    void getInsights_withCategoryFilter() throws Exception {
        mockMvc.perform(get("/api/insights?category=HOMESTEAD")
                        .header("Authorization", "Bearer " + accessToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data").isArray());
    }

    // ── Unread Count ─────────────────────────────────────────────────────

    @Test
    void getInsightUnreadCount_returnsZero() throws Exception {
        mockMvc.perform(get("/api/insights/unread-count")
                        .header("Authorization", "Bearer " + accessToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.unreadCount").value(0));
    }

    // ── Generate Insights ────────────────────────────────────────────────

    @Test
    void generateInsights_noData_returnsEmptyList() throws Exception {
        mockMvc.perform(post("/api/insights/generate")
                        .header("Authorization", "Bearer " + accessToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data").isArray());
    }

    // ── Mark Read (not found) ────────────────────────────────────────────

    @Test
    void markInsightRead_notFound_returns404() throws Exception {
        mockMvc.perform(put("/api/insights/" + UUID.randomUUID() + "/read")
                        .header("Authorization", "Bearer " + accessToken))
                .andExpect(status().isNotFound());
    }

    // ── Dismiss (not found) ──────────────────────────────────────────────

    @Test
    void dismissInsight_notFound_returns404() throws Exception {
        mockMvc.perform(put("/api/insights/" + UUID.randomUUID() + "/dismiss")
                        .header("Authorization", "Bearer " + accessToken))
                .andExpect(status().isNotFound());
    }

    // ── Unauthenticated ──────────────────────────────────────────────────

    @Test
    void insightEndpoints_unauthenticated_returns401() throws Exception {
        mockMvc.perform(get("/api/insights"))
                .andExpect(status().isUnauthorized());

        mockMvc.perform(post("/api/insights/generate"))
                .andExpect(status().isUnauthorized());

        mockMvc.perform(get("/api/insights/unread-count"))
                .andExpect(status().isUnauthorized());
    }

    // ── Pagination ───────────────────────────────────────────────────────

    @Test
    void getInsights_withPagination() throws Exception {
        mockMvc.perform(get("/api/insights?page=0&size=5")
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
