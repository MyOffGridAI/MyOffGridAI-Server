package com.myoffgridai.ai.dto;

import java.time.Instant;
import java.util.UUID;

/**
 * Data transfer object representing a conversation summary for listing views.
 *
 * @author MyOffGridAI
 * @since 0.1.0
 */
public record ConversationSummaryDto(
        UUID id,
        String title,
        boolean isArchived,
        int messageCount,
        Instant updatedAt,
        String lastMessagePreview
) {
}
