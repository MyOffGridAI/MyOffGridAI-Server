package com.myoffgridai.ai.dto;

import java.time.Instant;
import java.util.UUID;

/**
 * Data transfer object representing a conversation with full metadata.
 *
 * @author MyOffGridAI
 * @since 0.1.0
 */
public record ConversationDto(
        UUID id,
        String title,
        boolean isArchived,
        int messageCount,
        Instant createdAt,
        Instant updatedAt
) {
}
