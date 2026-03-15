package com.myoffgridai.events.service;

import com.myoffgridai.common.exception.EntityNotFoundException;
import com.myoffgridai.events.dto.CreateEventRequest;
import com.myoffgridai.events.dto.UpdateEventRequest;
import com.myoffgridai.events.model.*;
import com.myoffgridai.events.repository.ScheduledEventRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/**
 * Unit tests for {@link ScheduledEventService}.
 */
@ExtendWith(MockitoExtension.class)
class ScheduledEventServiceTest {

    @Mock
    private ScheduledEventRepository eventRepository;

    private ScheduledEventService eventService;
    private UUID userId;
    private UUID eventId;
    private ScheduledEvent testEvent;

    @BeforeEach
    void setUp() {
        eventService = new ScheduledEventService(eventRepository);
        userId = UUID.randomUUID();
        eventId = UUID.randomUUID();
        testEvent = createEvent(eventId, userId);
    }

    @Test
    void createEvent_scheduled_setsNextFireAt() {
        CreateEventRequest request = new CreateEventRequest(
                "Daily Report", "Generate daily report",
                EventType.SCHEDULED, "0 0 8 * * *", null,
                null, null, null,
                ActionType.AI_PROMPT, "Summarize today's events", true);

        when(eventRepository.save(any(ScheduledEvent.class)))
                .thenAnswer(inv -> {
                    ScheduledEvent e = inv.getArgument(0);
                    e.setId(eventId);
                    return e;
                });

        ScheduledEvent result = eventService.createEvent(userId, request);

        assertNotNull(result);
        assertEquals("Daily Report", result.getName());
        assertEquals(EventType.SCHEDULED, result.getEventType());
        assertNotNull(result.getNextFireAt());
        verify(eventRepository).save(any(ScheduledEvent.class));
    }

    @Test
    void createEvent_recurring_setsNextFireAt() {
        CreateEventRequest request = new CreateEventRequest(
                "Hourly Check", null,
                EventType.RECURRING, null, 60,
                null, null, null,
                ActionType.PUSH_NOTIFICATION, "Check sensors", true);

        when(eventRepository.save(any(ScheduledEvent.class)))
                .thenAnswer(inv -> {
                    ScheduledEvent e = inv.getArgument(0);
                    e.setId(eventId);
                    return e;
                });

        ScheduledEvent result = eventService.createEvent(userId, request);

        assertNotNull(result);
        assertEquals(EventType.RECURRING, result.getEventType());
        assertEquals(60, result.getRecurringIntervalMinutes());
        assertNotNull(result.getNextFireAt());
        verify(eventRepository).save(any(ScheduledEvent.class));
    }

    @Test
    void updateEvent_updatesAllFields() {
        when(eventRepository.findByIdAndUserId(eventId, userId))
                .thenReturn(Optional.of(testEvent));
        when(eventRepository.save(any(ScheduledEvent.class)))
                .thenAnswer(inv -> inv.getArgument(0));

        UpdateEventRequest request = new UpdateEventRequest(
                "Updated Name", "Updated desc",
                EventType.SCHEDULED, "0 0 12 * * *", null,
                null, null, null,
                ActionType.AI_SUMMARY, "Summarize", true);

        ScheduledEvent result = eventService.updateEvent(eventId, userId, request);

        assertEquals("Updated Name", result.getName());
        assertEquals("Updated desc", result.getDescription());
        assertEquals(ActionType.AI_SUMMARY, result.getActionType());
    }

    @Test
    void deleteEvent_deletesExistingEvent() {
        when(eventRepository.findByIdAndUserId(eventId, userId))
                .thenReturn(Optional.of(testEvent));

        eventService.deleteEvent(eventId, userId);

        verify(eventRepository).delete(testEvent);
    }

    @Test
    void toggleEvent_flipsEnabledState() {
        testEvent.setIsEnabled(true);
        when(eventRepository.findByIdAndUserId(eventId, userId))
                .thenReturn(Optional.of(testEvent));
        when(eventRepository.save(any(ScheduledEvent.class)))
                .thenAnswer(inv -> inv.getArgument(0));

        ScheduledEvent result = eventService.toggleEvent(eventId, userId);

        assertFalse(result.getIsEnabled());
    }

    @Test
    void getEvent_notFound_throwsEntityNotFoundException() {
        when(eventRepository.findByIdAndUserId(eventId, userId))
                .thenReturn(Optional.empty());

        assertThrows(EntityNotFoundException.class,
                () -> eventService.getEvent(eventId, userId));
    }

    @Test
    void listEvents_returnsPaginatedResults() {
        Page<ScheduledEvent> page = new PageImpl<>(
                List.of(testEvent), PageRequest.of(0, 20), 1);
        when(eventRepository.findAllByUserId(eq(userId), any(PageRequest.class)))
                .thenReturn(page);

        Page<ScheduledEvent> result = eventService.listEvents(userId, 0, 20);

        assertEquals(1, result.getTotalElements());
        assertEquals("Test Event", result.getContent().get(0).getName());
    }

    @Test
    void calculateNextFireAt_validCron_returnsInstant() {
        Instant next = eventService.calculateNextFireAt("0 0 8 * * *");
        assertNotNull(next);
        assertTrue(next.isAfter(Instant.now()));
    }

    @Test
    void calculateNextFireAt_invalidCron_returnsNull() {
        Instant next = eventService.calculateNextFireAt("not a cron");
        assertNull(next);
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
