package com.myoffgridai.privacy.dto;

import java.time.Instant;

/**
 * Data transfer object representing the current Fortress Mode status for data sovereignty.
 *
 * @author MyOffGridAI
 * @since 0.1.0
 */
public record FortressStatus(
        boolean enabled,
        Instant enabledAt,
        String enabledByUsername,
        boolean verified
) {}
