package com.myoffgridai.privacy.dto;

import java.time.Instant;

public record AuditSummary(
        long successCount,
        long failureCount,
        long deniedCount,
        Instant windowStart,
        Instant windowEnd
) {}
