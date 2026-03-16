package com.myoffgridai.ai.dto;

/**
 * Data transfer object representing the health status of the Ollama LLM service.
 *
 * @author MyOffGridAI
 * @since 0.1.0
 */
public record OllamaHealthDto(
        boolean available,
        String activeModel,
        String embedModelName,
        long responseTimeMs
) {
}
