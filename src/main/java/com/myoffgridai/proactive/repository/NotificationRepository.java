package com.myoffgridai.proactive.repository;

import com.myoffgridai.proactive.model.Notification;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Spring Data JPA repository for {@link Notification} entities.
 *
 * @author MyOffGridAI
 * @since 0.1.0
 */
@Repository
public interface NotificationRepository extends JpaRepository<Notification, UUID> {

    /**
     * Finds all unread notifications for a user, newest first.
     *
     * @param userId the user ID
     * @return list of unread notifications
     */
    List<Notification> findByUserIdAndIsReadFalseOrderByCreatedAtDesc(UUID userId);

    /**
     * Finds all notifications for a user with pagination, newest first.
     *
     * @param userId   the user ID
     * @param pageable the pagination parameters
     * @return paginated notifications
     */
    Page<Notification> findByUserIdOrderByCreatedAtDesc(UUID userId, Pageable pageable);

    /**
     * Counts unread notifications for a user.
     *
     * @param userId the user ID
     * @return the unread count
     */
    long countByUserIdAndIsReadFalse(UUID userId);

    /**
     * Finds a notification by ID scoped to a specific user.
     *
     * @param id     the notification ID
     * @param userId the user ID
     * @return the notification, or empty if not found
     */
    Optional<Notification> findByIdAndUserId(UUID id, UUID userId);

    /**
     * Marks all unread notifications as read for a user.
     *
     * @param userId the user ID
     * @param readAt the timestamp to set as the read time
     */
    @Modifying
    @Query("UPDATE Notification n SET n.isRead = true, n.readAt = :readAt WHERE n.userId = :userId AND n.isRead = false")
    void markAllReadForUser(@Param("userId") UUID userId, @Param("readAt") Instant readAt);

    /**
     * Deletes all notifications for a user.
     *
     * @param userId the user ID
     */
    @Modifying
    void deleteByUserId(UUID userId);
}
