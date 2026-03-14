package com.myoffgridai.memory.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;

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
