package com.myoffgridai.ai.dto;

public record OllamaHealthDto(
        boolean available,
        String activeModel,
        String embedModelName,
        long responseTimeMs
) {
}
