package com.myoffgridai.proactive.dto;

import com.myoffgridai.proactive.model.Notification;
import com.myoffgridai.proactive.model.NotificationSeverity;
import com.myoffgridai.proactive.model.NotificationType;

import java.time.Instant;
import java.util.UUID;

/**
 * Data transfer object representing a user notification from the proactive engine.
 *
 * @author MyOffGridAI
 * @since 0.1.0
 */
public record NotificationDto(
        UUID id,
        String title,
        String body,
        NotificationType type,
        NotificationSeverity severity,
        boolean isRead,
        Instant createdAt,
        Instant readAt,
        String metadata
) {

    public static NotificationDto from(Notification notification) {
        return new NotificationDto(
                notification.getId(),
                notification.getTitle(),
                notification.getBody(),
                notification.getType(),
                notification.getSeverity(),
                notification.getIsRead(),
                notification.getCreatedAt(),
                notification.getReadAt(),
                notification.getMetadata()
        );
    }
}
