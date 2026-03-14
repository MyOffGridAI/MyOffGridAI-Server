package com.myoffgridai.proactive.service;

import com.myoffgridai.common.exception.EntityNotFoundException;
import com.myoffgridai.proactive.model.Notification;
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
import java.util.UUID;

/**
 * Central notification hub. All services call this to create and deliver notifications.
 */
@Service
public class NotificationService {

    private static final Logger log = LoggerFactory.getLogger(NotificationService.class);

    private final NotificationRepository notificationRepository;
    private final NotificationSseRegistry sseRegistry;

    /**
     * Constructs the notification service.
     *
     * @param notificationRepository the notification repository
     * @param sseRegistry            the SSE registry for real-time push
     */
    public NotificationService(NotificationRepository notificationRepository,
                               NotificationSseRegistry sseRegistry) {
        this.notificationRepository = notificationRepository;
        this.sseRegistry = sseRegistry;
    }

    /**
     * Creates a notification, persists it, and broadcasts via SSE.
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
        Notification notification = new Notification();
        notification.setUserId(userId);
        notification.setTitle(title);
        notification.setBody(body);
        notification.setType(type);
        notification.setMetadata(metadata);

        notification = notificationRepository.save(notification);
        log.info("Created {} notification for user {}: {}", type, userId, title);

        sseRegistry.broadcast(userId, notification);
        return notification;
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
