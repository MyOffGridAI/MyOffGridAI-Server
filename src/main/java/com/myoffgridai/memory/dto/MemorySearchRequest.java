package com.myoffgridai.memory.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;

/**
 * Request body for performing a semantic search across user memories.
 *
 * @author MyOffGridAI
 * @since 0.1.0
 */
public record MemorySearchRequest(
        @NotBlank String query,
        @Min(1) @Max(20) int topK
) {
    public MemorySearchRequest {
        if (topK == 0) {
            topK = 5;
        }
    }
}
