package com.myoffgridai.notification.dto;

import java.time.Instant;
import java.util.Map;

/**
 * Payload published to MQTT topics for push notification delivery.
 *
 * <p>Serialized as JSON and sent to user-specific or broadcast topics.
 * Clients (Flutter app) subscribe to their topic and display the notification.</p>
 *
 * @param notificationId the persisted notification UUID
 * @param type           the notification type (matches {@code NotificationType} enum)
 * @param title          the notification title
 * @param body           the notification body
 * @param severity       the severity level (INFO, WARNING, CRITICAL)
 * @param timestamp      the notification creation timestamp
 * @param metadata       optional extra data (e.g., sensorId, alertId)
 */
public record NotificationPayload(
        String notificationId,
        String type,
        String title,
        String body,
        String severity,
        Instant timestamp,
        Map<String, String> metadata
) {
}
