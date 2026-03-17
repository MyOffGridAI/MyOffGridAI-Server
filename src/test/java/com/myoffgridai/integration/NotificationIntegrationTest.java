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

class NotificationIntegrationTest extends BaseIntegrationTest {

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
        accessToken = registerAndLogin("notifuser_" + System.nanoTime(), "pass");
    }

    // ── Get Notifications ────────────────────────────────────────────────

    @Test
    void getNotifications_emptyList() throws Exception {
        mockMvc.perform(get("/api/notifications")
                        .header("Authorization", "Bearer " + accessToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data").isArray());
    }

    @Test
    void getNotifications_unreadOnly_emptyList() throws Exception {
        mockMvc.perform(get("/api/notifications?unreadOnly=true")
                        .header("Authorization", "Bearer " + accessToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data").isArray());
    }

    // ── Unread Count ─────────────────────────────────────────────────────

    @Test
    void getNotificationUnreadCount_returnsZero() throws Exception {
        mockMvc.perform(get("/api/notifications/unread-count")
                        .header("Authorization", "Bearer " + accessToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.unreadCount").value(0));
    }

    // ── Mark All Read ────────────────────────────────────────────────────

    @Test
    void markAllRead_emptyList_succeeds() throws Exception {
        mockMvc.perform(put("/api/notifications/read-all")
                        .header("Authorization", "Bearer " + accessToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("All notifications marked as read"));
    }

    // ── Mark Single Read (not found) ─────────────────────────────────────

    @Test
    void markNotificationRead_notFound_returns404() throws Exception {
        mockMvc.perform(put("/api/notifications/" + UUID.randomUUID() + "/read")
                        .header("Authorization", "Bearer " + accessToken))
                .andExpect(status().isNotFound());
    }

    // ── Delete Notification (not found) ──────────────────────────────────

    @Test
    void deleteNotification_notFound_returns404() throws Exception {
        mockMvc.perform(delete("/api/notifications/" + UUID.randomUUID())
                        .header("Authorization", "Bearer " + accessToken))
                .andExpect(status().isNotFound());
    }

    // ── Unauthenticated ──────────────────────────────────────────────────

    @Test
    void notificationEndpoints_unauthenticated_returns401() throws Exception {
        mockMvc.perform(get("/api/notifications"))
                .andExpect(status().isUnauthorized());

        mockMvc.perform(get("/api/notifications/unread-count"))
                .andExpect(status().isUnauthorized());

        mockMvc.perform(put("/api/notifications/read-all"))
                .andExpect(status().isUnauthorized());
    }

    // ── SSE Stream ───────────────────────────────────────────────────────

    @Test
    void streamNotifications_returnsOk() throws Exception {
        mockMvc.perform(get("/api/notifications/stream")
                        .header("Authorization", "Bearer " + accessToken)
                        .accept(MediaType.TEXT_EVENT_STREAM_VALUE))
                .andExpect(status().isOk());
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
