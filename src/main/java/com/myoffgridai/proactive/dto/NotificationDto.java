package com.myoffgridai.proactive.dto;

import com.myoffgridai.proactive.model.Notification;
import com.myoffgridai.proactive.model.NotificationType;

import java.time.Instant;
import java.util.UUID;

public record NotificationDto(
        UUID id,
        String title,
        String body,
        NotificationType type,
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
                notification.getIsRead(),
                notification.getCreatedAt(),
                notification.getReadAt(),
                notification.getMetadata()
        );
    }
}
