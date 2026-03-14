package com.myoffgridai.privacy.dto;

import java.time.Instant;

public record FortressStatus(
        boolean enabled,
        Instant enabledAt,
        String enabledByUsername,
        boolean verified
) {}
