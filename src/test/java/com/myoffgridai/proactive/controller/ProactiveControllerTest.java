package com.myoffgridai.proactive.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.myoffgridai.auth.model.Role;
import com.myoffgridai.auth.model.User;
import com.myoffgridai.auth.service.AuthService;
import com.myoffgridai.auth.service.JwtService;
import com.myoffgridai.config.CaptivePortalRedirectFilter;
import com.myoffgridai.config.JwtAuthFilter;
import com.myoffgridai.config.TestSecurityConfig;
import com.myoffgridai.proactive.model.Insight;
import com.myoffgridai.proactive.model.InsightCategory;
import com.myoffgridai.proactive.model.Notification;
import com.myoffgridai.proactive.model.NotificationSeverity;
import com.myoffgridai.proactive.model.NotificationType;
import com.myoffgridai.proactive.service.InsightGeneratorService;
import com.myoffgridai.proactive.service.InsightService;
import com.myoffgridai.proactive.service.NotificationService;
import com.myoffgridai.proactive.service.NotificationSseRegistry;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.data.domain.PageImpl;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(ProactiveController.class)
@Import(TestSecurityConfig.class)
class ProactiveControllerTest {

    @Autowired private MockMvc mockMvc;
    @Autowired private ObjectMapper objectMapper;

    @MockitoBean private InsightService insightService;
    @MockitoBean private InsightGeneratorService insightGeneratorService;
    @MockitoBean private NotificationService notificationService;
    @MockitoBean private NotificationSseRegistry notificationSseRegistry;
    @MockitoBean private JwtService jwtService;
    @MockitoBean private JwtAuthFilter jwtAuthFilter;
    @MockitoBean private AuthService authService;
    @MockitoBean private CaptivePortalRedirectFilter captivePortalRedirectFilter;

    private User testUser;
    private UUID userId;

    @BeforeEach
    void setUp() {
        userId = UUID.randomUUID();
        testUser = new User();
        testUser.setId(userId);
        testUser.setUsername("testuser");
        testUser.setDisplayName("Test User");
        testUser.setRole(Role.ROLE_MEMBER);
        testUser.setPasswordHash("hash");
    }

    // ── Insights ─────────────────────────────────────────────────────────────

    @Test
    void getInsights_returnsInsightList() throws Exception {
        Insight insight = createInsight();
        when(insightService.getInsights(eq(userId), any()))
                .thenReturn(new PageImpl<>(List.of(insight)));

        mockMvc.perform(get("/api/insights").with(user(testUser)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data[0].content").value("Test insight"))
                .andExpect(jsonPath("$.data[0].category").value("GENERAL"))
                .andExpect(jsonPath("$.totalElements").value(1));
    }

    @Test
    void getInsights_withCategory_filtersResults() throws Exception {
        Insight insight = createInsight();
        insight.setCategory(InsightCategory.HOMESTEAD);
        when(insightService.getInsightsByCategory(eq(userId), eq(InsightCategory.HOMESTEAD), any()))
                .thenReturn(new PageImpl<>(List.of(insight)));

        mockMvc.perform(get("/api/insights?category=HOMESTEAD").with(user(testUser)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data[0].category").value("HOMESTEAD"));
    }

    @Test
    void getInsights_unauthenticated_returns401() throws Exception {
        mockMvc.perform(get("/api/insights"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void generateInsights_returnsGeneratedInsights() throws Exception {
        Insight insight = createInsight();
        when(insightGeneratorService.generateInsightForUser(userId))
                .thenReturn(List.of(insight));

        mockMvc.perform(post("/api/insights/generate").with(user(testUser)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data[0].content").value("Test insight"))
                .andExpect(jsonPath("$.message").value("Generated 1 insights"));
    }

    @Test
    void markInsightRead_returnsUpdated() throws Exception {
        Insight insight = createInsight();
        insight.setIsRead(true);
        insight.setReadAt(Instant.now());
        when(insightService.markRead(insight.getId(), userId)).thenReturn(insight);

        mockMvc.perform(put("/api/insights/" + insight.getId() + "/read").with(user(testUser)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.isRead").value(true))
                .andExpect(jsonPath("$.message").value("Insight marked as read"));
    }

    @Test
    void dismissInsight_returnsUpdated() throws Exception {
        Insight insight = createInsight();
        insight.setIsDismissed(true);
        when(insightService.dismiss(insight.getId(), userId)).thenReturn(insight);

        mockMvc.perform(put("/api/insights/" + insight.getId() + "/dismiss").with(user(testUser)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.isDismissed").value(true))
                .andExpect(jsonPath("$.message").value("Insight dismissed"));
    }

    @Test
    void getInsightUnreadCount_returnsCount() throws Exception {
        when(insightService.getUnreadCount(userId)).thenReturn(3L);

        mockMvc.perform(get("/api/insights/unread-count").with(user(testUser)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.unreadCount").value(3));
    }

    // ── Notifications ────────────────────────────────────────────────────────

    @Test
    void getNotifications_returnsPaginated() throws Exception {
        Notification notification = createNotification();
        when(notificationService.getNotifications(eq(userId), any()))
                .thenReturn(new PageImpl<>(List.of(notification)));

        mockMvc.perform(get("/api/notifications").with(user(testUser)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data[0].title").value("Test Title"))
                .andExpect(jsonPath("$.data[0].severity").value("WARNING"))
                .andExpect(jsonPath("$.totalElements").value(1));
    }

    @Test
    void getNotifications_unreadOnly_returnsList() throws Exception {
        Notification notification = createNotification();
        when(notificationService.getUnreadNotifications(userId))
                .thenReturn(List.of(notification));

        mockMvc.perform(get("/api/notifications?unreadOnly=true").with(user(testUser)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data[0].title").value("Test Title"));
    }

    @Test
    void getNotifications_unauthenticated_returns401() throws Exception {
        mockMvc.perform(get("/api/notifications"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void markNotificationRead_returnsUpdated() throws Exception {
        Notification notification = createNotification();
        notification.setIsRead(true);
        notification.setReadAt(Instant.now());
        when(notificationService.markRead(notification.getId(), userId)).thenReturn(notification);

        mockMvc.perform(put("/api/notifications/" + notification.getId() + "/read")
                        .with(user(testUser)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.isRead").value(true))
                .andExpect(jsonPath("$.message").value("Notification marked as read"));
    }

    @Test
    void markAllNotificationsRead_returnsSuccess() throws Exception {
        doNothing().when(notificationService).markAllRead(userId);

        mockMvc.perform(put("/api/notifications/read-all").with(user(testUser)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("All notifications marked as read"));

        verify(notificationService).markAllRead(userId);
    }

    @Test
    void getNotificationUnreadCount_returnsCount() throws Exception {
        when(notificationService.getUnreadCount(userId)).thenReturn(7L);

        mockMvc.perform(get("/api/notifications/unread-count").with(user(testUser)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.unreadCount").value(7));
    }

    @Test
    void deleteNotification_returnsSuccess() throws Exception {
        UUID notifId = UUID.randomUUID();
        doNothing().when(notificationService).deleteNotification(notifId, userId);

        mockMvc.perform(delete("/api/notifications/" + notifId).with(user(testUser)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("Notification deleted"));

        verify(notificationService).deleteNotification(notifId, userId);
    }

    @Test
    void streamNotifications_returnsSseEmitter() throws Exception {
        mockMvc.perform(get("/api/notifications/stream")
                        .with(user(testUser))
                        .accept(MediaType.TEXT_EVENT_STREAM_VALUE))
                .andExpect(status().isOk());

        verify(notificationSseRegistry).register(eq(userId), any());
    }

    // ── Helpers ──────────────────────────────────────────────────────────────

    private Insight createInsight() {
        Insight insight = new Insight();
        insight.setId(UUID.randomUUID());
        insight.setUserId(userId);
        insight.setContent("Test insight");
        insight.setCategory(InsightCategory.GENERAL);
        insight.setGeneratedAt(Instant.now());
        return insight;
    }

    private Notification createNotification() {
        Notification n = new Notification();
        n.setId(UUID.randomUUID());
        n.setUserId(userId);
        n.setTitle("Test Title");
        n.setBody("Test body");
        n.setType(NotificationType.GENERAL);
        n.setSeverity(NotificationSeverity.WARNING);
        n.setCreatedAt(Instant.now());
        return n;
    }
}
