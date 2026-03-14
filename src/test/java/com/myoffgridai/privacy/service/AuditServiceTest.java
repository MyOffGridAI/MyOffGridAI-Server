package com.myoffgridai.privacy.service;

import com.myoffgridai.privacy.model.AuditLog;
import com.myoffgridai.privacy.model.AuditOutcome;
import com.myoffgridai.privacy.repository.AuditLogRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * Unit tests for {@link AuditService}.
 */
@ExtendWith(MockitoExtension.class)
class AuditServiceTest {

    @Mock
    private AuditLogRepository auditLogRepository;

    private AuditService auditService;

    @BeforeEach
    void setUp() {
        auditService = new AuditService(auditLogRepository);
    }

    @Test
    void logAction_savesAuditLog() {
        AuditLog auditLog = new AuditLog();
        auditLog.setAction("GET /api/v1/test");
        auditLog.setOutcome(AuditOutcome.SUCCESS);

        auditService.logAction(auditLog);

        verify(auditLogRepository).save(auditLog);
    }

    @Test
    void getAuditLogs_returnsPaginatedLogs() {
        Pageable pageable = PageRequest.of(0, 10);
        AuditLog log1 = new AuditLog();
        log1.setAction("GET /api/v1/test");
        AuditLog log2 = new AuditLog();
        log2.setAction("POST /api/v1/other");
        Page<AuditLog> expectedPage = new PageImpl<>(List.of(log1, log2), pageable, 2);

        when(auditLogRepository.findAllByOrderByTimestampDesc(pageable)).thenReturn(expectedPage);

        Page<AuditLog> result = auditService.getAuditLogs(pageable);

        assertEquals(2, result.getTotalElements());
        assertEquals(log1, result.getContent().get(0));
        assertEquals(log2, result.getContent().get(1));
        verify(auditLogRepository).findAllByOrderByTimestampDesc(pageable);
    }

    @Test
    void getAuditLogsForUser_filtersByUser() {
        UUID userId = UUID.randomUUID();
        Pageable pageable = PageRequest.of(0, 10);
        AuditLog log1 = new AuditLog();
        log1.setUserId(userId);
        log1.setAction("GET /api/v1/test");
        Page<AuditLog> expectedPage = new PageImpl<>(List.of(log1), pageable, 1);

        when(auditLogRepository.findByUserIdOrderByTimestampDesc(userId, pageable)).thenReturn(expectedPage);

        Page<AuditLog> result = auditService.getAuditLogsForUser(userId, pageable);

        assertEquals(1, result.getTotalElements());
        assertEquals(userId, result.getContent().get(0).getUserId());
        verify(auditLogRepository).findByUserIdOrderByTimestampDesc(userId, pageable);
    }

    @Test
    void getAuditLogsByOutcome_filtersByOutcome() {
        Pageable pageable = PageRequest.of(0, 10);
        AuditLog log1 = new AuditLog();
        log1.setOutcome(AuditOutcome.FAILURE);
        Page<AuditLog> expectedPage = new PageImpl<>(List.of(log1), pageable, 1);

        when(auditLogRepository.findByOutcomeOrderByTimestampDesc(AuditOutcome.FAILURE, pageable))
                .thenReturn(expectedPage);

        Page<AuditLog> result = auditService.getAuditLogsByOutcome(AuditOutcome.FAILURE, pageable);

        assertEquals(1, result.getTotalElements());
        assertEquals(AuditOutcome.FAILURE, result.getContent().get(0).getOutcome());
        verify(auditLogRepository).findByOutcomeOrderByTimestampDesc(AuditOutcome.FAILURE, pageable);
    }

    @Test
    void getAuditLogsBetween_filtersByTimeRange() {
        Instant from = Instant.parse("2026-01-01T00:00:00Z");
        Instant to = Instant.parse("2026-01-31T23:59:59Z");
        Pageable pageable = PageRequest.of(0, 10);
        AuditLog log1 = new AuditLog();
        log1.setTimestamp(Instant.parse("2026-01-15T12:00:00Z"));
        Page<AuditLog> expectedPage = new PageImpl<>(List.of(log1), pageable, 1);

        when(auditLogRepository.findByTimestampBetweenOrderByTimestampDesc(from, to, pageable))
                .thenReturn(expectedPage);

        Page<AuditLog> result = auditService.getAuditLogsBetween(from, to, pageable);

        assertEquals(1, result.getTotalElements());
        verify(auditLogRepository).findByTimestampBetweenOrderByTimestampDesc(from, to, pageable);
    }

    @Test
    void countByOutcomeBetween_returnsCount() {
        Instant from = Instant.parse("2026-01-01T00:00:00Z");
        Instant to = Instant.parse("2026-01-31T23:59:59Z");

        when(auditLogRepository.countByOutcomeAndTimestampBetween(AuditOutcome.DENIED, from, to))
                .thenReturn(42L);

        long result = auditService.countByOutcomeBetween(AuditOutcome.DENIED, from, to);

        assertEquals(42L, result);
        verify(auditLogRepository).countByOutcomeAndTimestampBetween(AuditOutcome.DENIED, from, to);
    }

    @Test
    void deleteByUserId_delegatesToRepository() {
        UUID userId = UUID.randomUUID();

        auditService.deleteByUserId(userId);

        verify(auditLogRepository).deleteByUserId(userId);
    }
}
