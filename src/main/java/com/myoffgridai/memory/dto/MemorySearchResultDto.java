package com.myoffgridai.memory.dto;

/**
 * Data transfer object pairing a memory with its semantic similarity score from a vector search.
 *
 * @author MyOffGridAI
 * @since 0.1.0
 */
public record MemorySearchResultDto(
        MemoryDto memory,
        float similarityScore
) {}
