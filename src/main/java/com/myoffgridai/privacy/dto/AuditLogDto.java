package com.myoffgridai.privacy.dto;

import com.myoffgridai.privacy.model.AuditLog;
import com.myoffgridai.privacy.model.AuditOutcome;

import java.time.Instant;
import java.util.UUID;

public record AuditLogDto(
        UUID id,
        UUID userId,
        String username,
        String action,
        String resourceType,
        String resourceId,
        String httpMethod,
        String requestPath,
        AuditOutcome outcome,
        int responseStatus,
        long durationMs,
        Instant timestamp
) {
    public static AuditLogDto from(AuditLog log) {
        return new AuditLogDto(
                log.getId(),
                log.getUserId(),
                log.getUsername(),
                log.getAction(),
                log.getResourceType(),
                log.getResourceId(),
                log.getHttpMethod(),
                log.getRequestPath(),
                log.getOutcome(),
                log.getResponseStatus(),
                log.getDurationMs(),
                log.getTimestamp()
        );
    }
}
