package com.myoffgridai.ai.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

/**
 * A single streaming chunk from the Ollama chat API response.
 *
 * @author MyOffGridAI
 * @since 0.1.0
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public record OllamaChatChunk(
        OllamaMessage message,
        boolean done
) {
}
