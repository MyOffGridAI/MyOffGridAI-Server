package com.myoffgridai.events.dto;

import com.myoffgridai.events.model.ActionType;
import com.myoffgridai.events.model.EventType;
import com.myoffgridai.events.model.ThresholdOperator;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.util.UUID;

/**
 * Request payload for updating an existing scheduled event.
 */
public record UpdateEventRequest(
        @NotBlank String name,
        String description,
        @NotNull EventType eventType,
        String cronExpression,
        Integer recurringIntervalMinutes,
        UUID sensorId,
        ThresholdOperator thresholdOperator,
        Double thresholdValue,
        @NotNull ActionType actionType,
        @NotBlank String actionPayload,
        Boolean isEnabled
) {
}
