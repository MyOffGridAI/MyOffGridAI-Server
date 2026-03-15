package com.myoffgridai.events.service;

import com.myoffgridai.common.exception.EntityNotFoundException;
import com.myoffgridai.config.AppConstants;
import com.myoffgridai.events.dto.CreateEventRequest;
import com.myoffgridai.events.dto.UpdateEventRequest;
import com.myoffgridai.events.model.EventType;
import com.myoffgridai.events.model.ScheduledEvent;
import com.myoffgridai.events.repository.ScheduledEventRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.scheduling.support.CronExpression;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.UUID;

/**
 * Service for managing scheduled events.
 *
 * <p>Provides CRUD operations, toggle enable/disable, and
 * next-fire-at calculation for cron and recurring events.</p>
 */
@Service
public class ScheduledEventService {

    private static final Logger log = LoggerFactory.getLogger(ScheduledEventService.class);

    private final ScheduledEventRepository eventRepository;

    public ScheduledEventService(ScheduledEventRepository eventRepository) {
        this.eventRepository = eventRepository;
    }

    /**
     * Lists events for a user with pagination.
     */
    public Page<ScheduledEvent> listEvents(UUID userId, int page, int size) {
        size = Math.min(size, AppConstants.MAX_PAGE_SIZE);
        return eventRepository.findAllByUserId(userId,
                PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "createdAt")));
    }

    /**
     * Gets a single event by ID, scoped to the given user.
     *
     * @throws EntityNotFoundException if no matching event is found
     */
    public ScheduledEvent getEvent(UUID eventId, UUID userId) {
        return findByIdAndUser(eventId, userId);
    }

    /**
     * Creates a new scheduled event for the given user.
     */
    public ScheduledEvent createEvent(UUID userId, CreateEventRequest request) {
        ScheduledEvent event = new ScheduledEvent();
        event.setUserId(userId);
        event.setName(request.name());
        event.setDescription(request.description());
        event.setEventType(request.eventType());
        event.setIsEnabled(request.isEnabled() != null ? request.isEnabled() : true);
        event.setCronExpression(request.cronExpression());
        event.setRecurringIntervalMinutes(request.recurringIntervalMinutes());
        event.setSensorId(request.sensorId());
        event.setThresholdOperator(request.thresholdOperator());
        event.setThresholdValue(request.thresholdValue());
        event.setActionType(request.actionType());
        event.setActionPayload(request.actionPayload());

        if (request.eventType() == EventType.SCHEDULED && request.cronExpression() != null) {
            event.setNextFireAt(calculateNextFireAt(request.cronExpression()));
        } else if (request.eventType() == EventType.RECURRING && request.recurringIntervalMinutes() != null) {
            event.setNextFireAt(Instant.now().plusSeconds(request.recurringIntervalMinutes() * 60L));
        }

        event = eventRepository.save(event);
        log.info("Created event '{}' (type={}) for user {}", event.getName(), event.getEventType(), userId);
        return event;
    }

    /**
     * Updates an existing scheduled event.
     *
     * @throws EntityNotFoundException if no matching event is found
     */
    public ScheduledEvent updateEvent(UUID eventId, UUID userId, UpdateEventRequest request) {
        ScheduledEvent event = findByIdAndUser(eventId, userId);
        event.setName(request.name());
        event.setDescription(request.description());
        event.setEventType(request.eventType());
        event.setIsEnabled(request.isEnabled() != null ? request.isEnabled() : event.getIsEnabled());
        event.setCronExpression(request.cronExpression());
        event.setRecurringIntervalMinutes(request.recurringIntervalMinutes());
        event.setSensorId(request.sensorId());
        event.setThresholdOperator(request.thresholdOperator());
        event.setThresholdValue(request.thresholdValue());
        event.setActionType(request.actionType());
        event.setActionPayload(request.actionPayload());

        if (request.eventType() == EventType.SCHEDULED && request.cronExpression() != null) {
            event.setNextFireAt(calculateNextFireAt(request.cronExpression()));
        } else if (request.eventType() == EventType.RECURRING && request.recurringIntervalMinutes() != null) {
            event.setNextFireAt(Instant.now().plusSeconds(request.recurringIntervalMinutes() * 60L));
        } else {
            event.setNextFireAt(null);
        }

        event = eventRepository.save(event);
        log.info("Updated event '{}' for user {}", event.getName(), userId);
        return event;
    }

    /**
     * Deletes an event by ID, scoped to the given user.
     *
     * @throws EntityNotFoundException if no matching event is found
     */
    @Transactional
    public void deleteEvent(UUID eventId, UUID userId) {
        ScheduledEvent event = findByIdAndUser(eventId, userId);
        eventRepository.delete(event);
        log.info("Deleted event '{}' for user {}", event.getName(), userId);
    }

    /**
     * Toggles the enabled state of an event.
     *
     * @throws EntityNotFoundException if no matching event is found
     */
    public ScheduledEvent toggleEvent(UUID eventId, UUID userId) {
        ScheduledEvent event = findByIdAndUser(eventId, userId);
        event.setIsEnabled(!event.getIsEnabled());
        event = eventRepository.save(event);
        log.info("Toggled event '{}' to enabled={} for user {}",
                event.getName(), event.getIsEnabled(), userId);
        return event;
    }

    /**
     * Deletes all events for a user (used during user deletion).
     */
    @Transactional
    public void deleteAllForUser(UUID userId) {
        eventRepository.deleteByUserId(userId);
        log.info("Deleted all events for user {}", userId);
    }

    /**
     * Calculates the next fire time from a Spring 6-field cron expression.
     *
     * @param cronExpr a 6-field cron expression (second minute hour day month weekday)
     * @return the next fire instant, or null if the expression is invalid
     */
    Instant calculateNextFireAt(String cronExpr) {
        try {
            CronExpression cron = CronExpression.parse(cronExpr);
            LocalDateTime next = cron.next(LocalDateTime.now(ZoneOffset.UTC));
            return next != null ? next.toInstant(ZoneOffset.UTC) : null;
        } catch (IllegalArgumentException e) {
            log.warn("Invalid cron expression '{}': {}", cronExpr, e.getMessage());
            return null;
        }
    }

    private ScheduledEvent findByIdAndUser(UUID eventId, UUID userId) {
        return eventRepository.findByIdAndUserId(eventId, userId)
                .orElseThrow(() -> new EntityNotFoundException("Event not found: " + eventId));
    }
}
