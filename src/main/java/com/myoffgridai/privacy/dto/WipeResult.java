package com.myoffgridai.privacy.dto;

import java.time.Instant;
import java.util.UUID;

/**
 * Data transfer object representing the result of a user data wipe operation.
 *
 * @author MyOffGridAI
 * @since 0.1.0
 */
public record WipeResult(
        UUID targetUserId,
        int stepsCompleted,
        Instant completedAt,
        boolean success
) {}
