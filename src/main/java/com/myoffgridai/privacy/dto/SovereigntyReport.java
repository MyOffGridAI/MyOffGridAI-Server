package com.myoffgridai.privacy.dto;

import java.time.Instant;

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
