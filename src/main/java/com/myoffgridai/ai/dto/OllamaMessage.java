package com.myoffgridai.ai.dto;

/**
 * Represents a single message in the Ollama chat API format with a role and content.
 *
 * @author MyOffGridAI
 * @since 0.1.0
 */
public record OllamaMessage(
        String role,
        String content
) {
}
