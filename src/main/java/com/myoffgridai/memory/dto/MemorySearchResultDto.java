package com.myoffgridai.memory.dto;

public record MemorySearchResultDto(
        MemoryDto memory,
        float similarityScore
) {}
