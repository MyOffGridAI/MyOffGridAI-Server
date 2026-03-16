package com.myoffgridai.proactive.service;

import com.myoffgridai.common.exception.EntityNotFoundException;
import com.myoffgridai.notification.dto.NotificationPayload;
import com.myoffgridai.notification.service.DeviceRegistrationService;
import com.myoffgridai.notification.service.MqttPublisherService;
import com.myoffgridai.proactive.model.Notification;
import com.myoffgridai.proactive.model.NotificationSeverity;
import com.myoffgridai.proactive.model.NotificationType;
import com.myoffgridai.proactive.repository.NotificationRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Central notification hub. All services call this to create and deliver notifications.
 */
@Service
public class NotificationService {

    private static final Logger log = LoggerFactory.getLogger(NotificationService.class);

    private final NotificationRepository notificationRepository;
    private final NotificationSseRegistry sseRegistry;
    private final MqttPublisherService mqttPublisherService;
    private final DeviceRegistrationService deviceRegistrationService;

    /**
     * Constructs the notification service.
     *
     * @param notificationRepository    the notification repository
     * @param sseRegistry               the SSE registry for real-time push
     * @param mqttPublisherService      the MQTT publisher for push delivery
     * @param deviceRegistrationService the device registration service for topic lookup
     */
    public NotificationService(NotificationRepository notificationRepository,
                               NotificationSseRegistry sseRegistry,
                               MqttPublisherService mqttPublisherService,
                               DeviceRegistrationService deviceRegistrationService) {
        this.notificationRepository = notificationRepository;
        this.sseRegistry = sseRegistry;
        this.mqttPublisherService = mqttPublisherService;
        this.deviceRegistrationService = deviceRegistrationService;
    }

    /**
     * Creates a notification, persists it, broadcasts via SSE, and publishes via MQTT.
     *
     * @param userId   the user ID
     * @param title    the notification title
     * @param body     the notification body
     * @param type     the notification type
     * @param metadata optional JSON metadata
     * @return the persisted notification
     */
    public Notification createNotification(UUID userId, String title, String body,
                                           NotificationType type, String metadata) {
        return createNotification(userId, title, body, type, null, metadata);
    }

    /**
     * Creates a notification with severity, persists it, broadcasts via SSE,
     * and publishes to MQTT topics for the user's registered devices.
     *
     * @param userId   the user ID
     * @param title    the notification title
     * @param body     the notification body
     * @param type     the notification type
     * @param severity the notification severity (nullable, defaults to INFO)
     * @param metadata optional JSON metadata
     * @return the persisted notification
     */
    public Notification createNotification(UUID userId, String title, String body,
                                           NotificationType type,
                                           NotificationSeverity severity,
                                           String metadata) {
        Notification notification = new Notification();
        notification.setUserId(userId);
        notification.setTitle(title);
        notification.setBody(body);
        notification.setType(type);
        notification.setSeverity(severity != null ? severity : NotificationSeverity.INFO);
        notification.setMetadata(metadata);

        notification = notificationRepository.save(notification);
        log.info("Created {} notification for user {}: {}", type, userId, title);

        sseRegistry.broadcast(userId, notification);

        // Publish via MQTT to all user's registered device topics
        boolean mqttSuccess = publishToMqtt(userId, notification);
        if (mqttSuccess) {
            notification.setMqttDelivered(true);
            notification = notificationRepository.save(notification);
        }

        return notification;
    }

    /**
     * Publishes a notification to all MQTT topics for the user's registered devices.
     *
     * @param userId       the user ID
     * @param notification the notification entity
     * @return true if at least one MQTT publish succeeded
     */
    private boolean publishToMqtt(UUID userId, Notification notification) {
        List<String> topics = deviceRegistrationService.getTopicsForUser(userId);
        if (topics.isEmpty()) {
            return false;
        }

        NotificationPayload payload = new NotificationPayload(
                notification.getId().toString(),
                notification.getType().name(),
                notification.getTitle(),
                notification.getBody(),
                notification.getSeverity() != null ? notification.getSeverity().name() : "INFO",
                notification.getCreatedAt(),
                null
        );

        boolean anySuccess = false;
        for (String topic : topics) {
            if (mqttPublisherService.publishToTopic(topic, payload)) {
                anySuccess = true;
            }
        }
        return anySuccess;
    }

    /**
     * Gets all unread notifications for a user, newest first.
     *
     * @param userId the user ID
     * @return list of unread notifications
     */
    public List<Notification> getUnreadNotifications(UUID userId) {
        return notificationRepository.findByUserIdAndIsReadFalseOrderByCreatedAtDesc(userId);
    }

    /**
     * Gets all notifications for a user with pagination.
     *
     * @param userId   the user ID
     * @param pageable the pagination parameters
     * @return paginated notifications
     */
    public Page<Notification> getNotifications(UUID userId, Pageable pageable) {
        return notificationRepository.findByUserIdOrderByCreatedAtDesc(userId, pageable);
    }

    /**
     * Marks a notification as read.
     *
     * @param notificationId the notification ID
     * @param userId         the user ID for ownership verification
     * @return the updated notification
     */
    public Notification markRead(UUID notificationId, UUID userId) {
        Notification notification = notificationRepository.findByIdAndUserId(notificationId, userId)
                .orElseThrow(() -> new EntityNotFoundException("Notification not found: " + notificationId));
        notification.setIsRead(true);
        notification.setReadAt(Instant.now());
        notification = notificationRepository.save(notification);

        long unreadCount = notificationRepository.countByUserIdAndIsReadFalse(userId);
        sseRegistry.broadcastUnreadCount(userId, unreadCount);
        return notification;
    }

    /**
     * Marks all notifications as read for a user.
     *
     * @param userId the user ID
     */
    @Transactional
    public void markAllRead(UUID userId) {
        notificationRepository.markAllReadForUser(userId, Instant.now());
        sseRegistry.broadcastUnreadCount(userId, 0);
        log.info("Marked all notifications read for user {}", userId);
    }

    /**
     * Gets the count of unread notifications for a user.
     *
     * @param userId the user ID
     * @return the unread count
     */
    public long getUnreadCount(UUID userId) {
        return notificationRepository.countByUserIdAndIsReadFalse(userId);
    }

    /**
     * Deletes a notification after verifying ownership.
     *
     * @param notificationId the notification ID
     * @param userId         the user ID
     */
    public void deleteNotification(UUID notificationId, UUID userId) {
        Notification notification = notificationRepository.findByIdAndUserId(notificationId, userId)
                .orElseThrow(() -> new EntityNotFoundException("Notification not found: " + notificationId));
        notificationRepository.delete(notification);
        log.info("Deleted notification {} for user {}", notificationId, userId);
    }

    /**
     * Deletes all notifications for a user. Used by privacy wipe.
     *
     * @param userId the user ID
     */
    @Transactional
    public void deleteAllForUser(UUID userId) {
        notificationRepository.deleteByUserId(userId);
        log.info("Deleted all notifications for user {}", userId);
    }
}
