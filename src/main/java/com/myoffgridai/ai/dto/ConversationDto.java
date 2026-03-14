package com.myoffgridai.ai.dto;

import java.time.Instant;
import java.util.UUID;

public record ConversationDto(
        UUID id,
        String title,
        boolean isArchived,
        int messageCount,
        Instant createdAt,
        Instant updatedAt
) {
}
