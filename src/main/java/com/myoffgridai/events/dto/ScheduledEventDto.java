package com.myoffgridai.events.dto;

import com.myoffgridai.events.model.*;

import java.time.Instant;
import java.util.UUID;

/**
 * Data transfer object for {@link ScheduledEvent}.
 *
 * <p>Used in API responses to expose event data without entity internals.</p>
 */
public record ScheduledEventDto(
        UUID id,
        UUID userId,
        String name,
        String description,
        EventType eventType,
        boolean isEnabled,
        String cronExpression,
        Integer recurringIntervalMinutes,
        UUID sensorId,
        ThresholdOperator thresholdOperator,
        Double thresholdValue,
        ActionType actionType,
        String actionPayload,
        Instant lastTriggeredAt,
        Instant nextFireAt,
        Instant createdAt,
        Instant updatedAt
) {

    /**
     * Converts a {@link ScheduledEvent} entity to a DTO.
     */
    public static ScheduledEventDto from(ScheduledEvent event) {
        return new ScheduledEventDto(
                event.getId(),
                event.getUserId(),
                event.getName(),
                event.getDescription(),
                event.getEventType(),
                event.getIsEnabled(),
                event.getCronExpression(),
                event.getRecurringIntervalMinutes(),
                event.getSensorId(),
                event.getThresholdOperator(),
                event.getThresholdValue(),
                event.getActionType(),
                event.getActionPayload(),
                event.getLastTriggeredAt(),
                event.getNextFireAt(),
                event.getCreatedAt(),
                event.getUpdatedAt()
        );
    }
}
