package com.myoffgridai.privacy.dto;

import java.time.Instant;
import java.util.UUID;

public record WipeResult(
        UUID targetUserId,
        int stepsCompleted,
        Instant completedAt,
        boolean success
) {}
