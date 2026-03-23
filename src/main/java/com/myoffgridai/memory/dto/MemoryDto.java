package com.myoffgridai.memory.dto;

import com.myoffgridai.memory.model.MemoryImportance;

import java.time.Instant;
import java.util.UUID;

/**
 * Data transfer object representing a user memory with metadata and access statistics.
 *
 * @author MyOffGridAI
 * @since 0.1.0
 */
public record MemoryDto(
        UUID id,
        UUID userId,
        String content,
        MemoryImportance importance,
        String tags,
        UUID sourceConversationId,
        Instant createdAt,
        Instant updatedAt,
        Instant lastAccessedAt,
        int accessCount,
        boolean shared
) {}
