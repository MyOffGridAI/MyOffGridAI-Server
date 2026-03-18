package com.myoffgridai.ai.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;

/**
 * Represents a single message in the Ollama chat API format with a role, content,
 * and optional thinking content.
 *
 * <p>Ollama 0.6+ separates reasoning model output into a dedicated {@code thinking}
 * field on the message object rather than embedding {@code <think>} tags in the
 * content stream.</p>
 *
 * @author MyOffGridAI
 * @since 0.1.0
 */
@JsonIgnoreProperties(ignoreUnknown = true)
@JsonInclude(JsonInclude.Include.NON_NULL)
public record OllamaMessage(
        String role,
        String content,
        String thinking
) {
    /**
     * Convenience constructor for outbound messages (no thinking field).
     */
    public OllamaMessage(String role, String content) {
        this(role, content, null);
    }
}
