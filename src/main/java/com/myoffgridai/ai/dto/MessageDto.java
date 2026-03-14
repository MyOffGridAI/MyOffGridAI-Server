package com.myoffgridai.ai.dto;

import com.myoffgridai.ai.model.MessageRole;

import java.time.Instant;
import java.util.UUID;

public record MessageDto(
        UUID id,
        MessageRole role,
        String content,
        Integer tokenCount,
        boolean hasRagContext,
        Instant createdAt
) {
}
