package com.myoffgridai.memory.dto;

import com.myoffgridai.memory.model.MemoryImportance;

import java.time.Instant;
import java.util.UUID;

public record MemoryDto(
        UUID id,
        String content,
        MemoryImportance importance,
        String tags,
        UUID sourceConversationId,
        Instant createdAt,
        Instant updatedAt,
        Instant lastAccessedAt,
        int accessCount
) {}
