package com.myoffgridai.privacy.dto;

import java.time.Instant;

/**
 * Data transfer object representing a comprehensive data sovereignty verification report.
 *
 * @author MyOffGridAI
 * @since 0.1.0
 */
public record SovereigntyReport(
        Instant generatedAt,
        FortressStatus fortressStatus,
        String outboundTrafficVerification,
        DataInventory dataInventory,
        AuditSummary auditSummary,
        String encryptionStatus,
        String telemetryStatus,
        Instant lastVerifiedAt
) {}
