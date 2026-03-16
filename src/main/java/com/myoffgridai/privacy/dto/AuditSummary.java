package com.myoffgridai.privacy.dto;

import java.time.Instant;

/**
 * Data transfer object summarizing audit log outcomes within a time window.
 *
 * @author MyOffGridAI
 * @since 0.1.0
 */
public record AuditSummary(
        long successCount,
        long failureCount,
        long deniedCount,
        Instant windowStart,
        Instant windowEnd
) {}
