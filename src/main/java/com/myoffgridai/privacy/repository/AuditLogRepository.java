package com.myoffgridai.privacy.repository;

import com.myoffgridai.privacy.model.AuditLog;
import com.myoffgridai.privacy.model.AuditOutcome;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.UUID;

/**
 * Spring Data JPA repository for {@link AuditLog} entities.
 *
 * @author MyOffGridAI
 * @since 0.1.0
 */
@Repository
public interface AuditLogRepository extends JpaRepository<AuditLog, UUID> {

    /**
     * Finds all audit logs with pagination, newest first.
     *
     * @param pageable the pagination parameters
     * @return paginated audit logs
     */
    Page<AuditLog> findAllByOrderByTimestampDesc(Pageable pageable);

    /**
     * Finds audit logs for a specific user, newest first.
     *
     * @param userId   the user ID
     * @param pageable the pagination parameters
     * @return paginated audit logs for the user
     */
    Page<AuditLog> findByUserIdOrderByTimestampDesc(UUID userId, Pageable pageable);

    /**
     * Finds audit logs filtered by outcome, newest first.
     *
     * @param outcome  the audit outcome filter
     * @param pageable the pagination parameters
     * @return paginated audit logs matching the outcome
     */
    Page<AuditLog> findByOutcomeOrderByTimestampDesc(AuditOutcome outcome, Pageable pageable);

    /**
     * Finds audit logs within a time range, newest first.
     *
     * @param from     the start of the time range
     * @param to       the end of the time range
     * @param pageable the pagination parameters
     * @return paginated audit logs within the range
     */
    Page<AuditLog> findByTimestampBetweenOrderByTimestampDesc(Instant from, Instant to, Pageable pageable);

    /**
     * Finds audit logs for a user within a time range.
     *
     * @param userId   the user ID
     * @param from     the start of the time range
     * @param to       the end of the time range
     * @param pageable the pagination parameters
     * @return paginated audit logs for the user within the range
     */
    Page<AuditLog> findByUserIdAndTimestampBetween(UUID userId, Instant from, Instant to, Pageable pageable);

    /**
     * Counts audit logs by outcome within a time range.
     *
     * @param outcome the audit outcome
     * @param from    the start of the time range
     * @param to      the end of the time range
     * @return the count of matching audit logs
     */
    long countByOutcomeAndTimestampBetween(AuditOutcome outcome, Instant from, Instant to);

    /**
     * Deletes audit logs older than the specified timestamp.
     *
     * @param before the cutoff timestamp
     */
    @Modifying
    void deleteByTimestampBefore(Instant before);

    /**
     * Deletes all audit logs for a user.
     *
     * @param userId the user ID
     */
    @Modifying
    void deleteByUserId(UUID userId);
}
