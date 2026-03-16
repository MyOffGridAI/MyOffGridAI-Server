package com.myoffgridai.ai.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * Response object returned by the Ollama chat API containing the assistant message and usage metadata.
 *
 * @author MyOffGridAI
 * @since 0.1.0
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public record OllamaChatResponse(
        OllamaMessage message,
        boolean done,
        @JsonProperty("total_duration") Long totalDuration,
        @JsonProperty("eval_count") Integer evalCount
) {
}
