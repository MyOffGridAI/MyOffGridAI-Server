package com.myoffgridai.events.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.myoffgridai.auth.model.Role;
import com.myoffgridai.auth.model.User;
import com.myoffgridai.auth.service.AuthService;
import com.myoffgridai.auth.service.JwtService;
import com.myoffgridai.config.CaptivePortalRedirectFilter;
import com.myoffgridai.config.JwtAuthFilter;
import com.myoffgridai.config.TestSecurityConfig;
import com.myoffgridai.events.dto.CreateEventRequest;
import com.myoffgridai.events.dto.UpdateEventRequest;
import com.myoffgridai.events.model.*;
import com.myoffgridai.events.service.ScheduledEventService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
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

/**
 * Web layer tests for {@link ScheduledEventController}.
 */
@WebMvcTest(ScheduledEventController.class)
@Import(TestSecurityConfig.class)
class ScheduledEventControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockitoBean
    private ScheduledEventService eventService;

    @MockitoBean
    private JwtService jwtService;

    @MockitoBean
    private JwtAuthFilter jwtAuthFilter;

    @MockitoBean
    private AuthService authService;

    @MockitoBean
    private CaptivePortalRedirectFilter captivePortalRedirectFilter;

    private User testUser;
    private UUID userId;
    private UUID eventId;
    private ScheduledEvent testEvent;

    @BeforeEach
    void setUp() {
        userId = UUID.randomUUID();
        eventId = UUID.randomUUID();
        testUser = new User();
        testUser.setId(userId);
        testUser.setUsername("testuser");
        testUser.setDisplayName("Test User");
        testUser.setRole(Role.ROLE_MEMBER);
        testUser.setPasswordHash("hash");
        testEvent = createEvent(eventId, userId);
    }

    @Test
    void listEvents_returnsPagedResults() throws Exception {
        var page = new PageImpl<>(List.of(testEvent), PageRequest.of(0, 20), 1);
        when(eventService.listEvents(eq(userId), eq(0), eq(20))).thenReturn(page);

        mockMvc.perform(get("/api/events")
                        .with(user(testUser)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data[0].name").value("Test Event"));
    }

    @Test
    void getEvent_returnsEvent() throws Exception {
        when(eventService.getEvent(eventId, userId)).thenReturn(testEvent);

        mockMvc.perform(get("/api/events/{id}", eventId)
                        .with(user(testUser)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.name").value("Test Event"));
    }

    @Test
    void createEvent_returnsCreatedEvent() throws Exception {
        when(eventService.createEvent(eq(userId), any(CreateEventRequest.class)))
                .thenReturn(testEvent);

        var request = new CreateEventRequest(
                "Test Event", "desc", EventType.SCHEDULED,
                "0 0 8 * * *", null, null, null, null,
                ActionType.PUSH_NOTIFICATION, "msg", true);

        mockMvc.perform(post("/api/events")
                        .with(user(testUser))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("Event created"))
                .andExpect(jsonPath("$.data.name").value("Test Event"));
    }

    @Test
    void updateEvent_returnsUpdatedEvent() throws Exception {
        when(eventService.updateEvent(eq(eventId), eq(userId), any(UpdateEventRequest.class)))
                .thenReturn(testEvent);

        var request = new UpdateEventRequest(
                "Test Event", "updated", EventType.SCHEDULED,
                "0 0 12 * * *", null, null, null, null,
                ActionType.AI_PROMPT, "prompt", true);

        mockMvc.perform(put("/api/events/{id}", eventId)
                        .with(user(testUser))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("Event updated"));
    }

    @Test
    void deleteEvent_returnsSuccess() throws Exception {
        doNothing().when(eventService).deleteEvent(eventId, userId);

        mockMvc.perform(delete("/api/events/{id}", eventId)
                        .with(user(testUser)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("Event deleted"));
    }

    @Test
    void toggleEvent_returnsToggledEvent() throws Exception {
        testEvent.setIsEnabled(false);
        when(eventService.toggleEvent(eventId, userId)).thenReturn(testEvent);

        mockMvc.perform(put("/api/events/{id}/toggle", eventId)
                        .with(user(testUser)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("Event disabled"));
    }

    @Test
    void createEvent_blankName_returns400() throws Exception {
        var request = new CreateEventRequest(
                "", null, EventType.SCHEDULED,
                null, null, null, null, null,
                ActionType.PUSH_NOTIFICATION, "msg", true);

        mockMvc.perform(post("/api/events")
                        .with(user(testUser))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest());
    }

    @Test
    void listEvents_unauthenticated_returns401() throws Exception {
        mockMvc.perform(get("/api/events"))
                .andExpect(status().isUnauthorized());
    }

    private ScheduledEvent createEvent(UUID id, UUID userId) {
        ScheduledEvent event = new ScheduledEvent();
        event.setId(id);
        event.setUserId(userId);
        event.setName("Test Event");
        event.setDescription("A test event");
        event.setEventType(EventType.SCHEDULED);
        event.setIsEnabled(true);
        event.setCronExpression("0 0 8 * * *");
        event.setActionType(ActionType.PUSH_NOTIFICATION);
        event.setActionPayload("Test message");
        event.setCreatedAt(Instant.now());
        event.setUpdatedAt(Instant.now());
        return event;
    }
}
