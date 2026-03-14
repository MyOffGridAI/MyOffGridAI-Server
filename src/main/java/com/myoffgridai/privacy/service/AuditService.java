package com.myoffgridai.privacy.service;

import com.myoffgridai.privacy.model.AuditLog;
import com.myoffgridai.privacy.model.AuditOutcome;
import com.myoffgridai.privacy.repository.AuditLogRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.UUID;

/**
 * Provides audit log persistence and query operations. All audit logging
 * flows through this service — no other code should call AuditLogRepository directly.
 */
@Service
public class AuditService {

    private static final Logger log = LoggerFactory.getLogger(AuditService.class);

    private final AuditLogRepository auditLogRepository;

    /**
     * Constructs the audit service.
     *
     * @param auditLogRepository the audit log repository
     */
    public AuditService(AuditLogRepository auditLogRepository) {
        this.auditLogRepository = auditLogRepository;
    }

    /**
     * Persists an audit log entry. Called by the AOP aspect on every controller invocation.
     *
     * @param auditLog the audit log to persist
     */
    public void logAction(AuditLog auditLog) {
        auditLogRepository.save(auditLog);
    }

    /**
     * Gets all audit logs with pagination, newest first.
     *
     * @param pageable the pagination parameters
     * @return paginated audit logs
     */
    public Page<AuditLog> getAuditLogs(Pageable pageable) {
        return auditLogRepository.findAllByOrderByTimestampDesc(pageable);
    }

    /**
     * Gets audit logs for a specific user with pagination.
     *
     * @param userId   the user ID
     * @param pageable the pagination parameters
     * @return paginated audit logs
     */
    public Page<AuditLog> getAuditLogsForUser(UUID userId, Pageable pageable) {
        return auditLogRepository.findByUserIdOrderByTimestampDesc(userId, pageable);
    }

    /**
     * Gets audit logs filtered by outcome with pagination.
     *
     * @param outcome  the audit outcome filter
     * @param pageable the pagination parameters
     * @return paginated audit logs
     */
    public Page<AuditLog> getAuditLogsByOutcome(AuditOutcome outcome, Pageable pageable) {
        return auditLogRepository.findByOutcomeOrderByTimestampDesc(outcome, pageable);
    }

    /**
     * Gets audit logs within a time range with pagination.
     *
     * @param from     the start of the time range
     * @param to       the end of the time range
     * @param pageable the pagination parameters
     * @return paginated audit logs
     */
    public Page<AuditLog> getAuditLogsBetween(Instant from, Instant to, Pageable pageable) {
        return auditLogRepository.findByTimestampBetweenOrderByTimestampDesc(from, to, pageable);
    }

    /**
     * Counts audit logs by outcome within a time range.
     *
     * @param outcome the audit outcome
     * @param from    the start of the time range
     * @param to      the end of the time range
     * @return the count
     */
    public long countByOutcomeBetween(AuditOutcome outcome, Instant from, Instant to) {
        return auditLogRepository.countByOutcomeAndTimestampBetween(outcome, from, to);
    }

    /**
     * Deletes all audit logs for a specific user. Used by privacy wipe.
     *
     * @param userId the user ID
     */
    @Transactional
    public void deleteByUserId(UUID userId) {
        auditLogRepository.deleteByUserId(userId);
        log.info("Deleted audit logs for user {}", userId);
    }
}
