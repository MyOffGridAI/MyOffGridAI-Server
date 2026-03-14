package com.myoffgridai.ai.dto;

import java.time.Instant;
import java.util.UUID;

public record ConversationSummaryDto(
        UUID id,
        String title,
        boolean isArchived,
        int messageCount,
        Instant updatedAt,
        String lastMessagePreview
) {
}
