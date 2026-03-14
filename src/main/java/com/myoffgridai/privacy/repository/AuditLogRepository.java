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

@Repository
public interface AuditLogRepository extends JpaRepository<AuditLog, UUID> {

    Page<AuditLog> findAllByOrderByTimestampDesc(Pageable pageable);

    Page<AuditLog> findByUserIdOrderByTimestampDesc(UUID userId, Pageable pageable);

    Page<AuditLog> findByOutcomeOrderByTimestampDesc(AuditOutcome outcome, Pageable pageable);

    Page<AuditLog> findByTimestampBetweenOrderByTimestampDesc(Instant from, Instant to, Pageable pageable);

    Page<AuditLog> findByUserIdAndTimestampBetween(UUID userId, Instant from, Instant to, Pageable pageable);

    long countByOutcomeAndTimestampBetween(AuditOutcome outcome, Instant from, Instant to);

    @Modifying
    void deleteByTimestampBefore(Instant before);

    @Modifying
    void deleteByUserId(UUID userId);
}
