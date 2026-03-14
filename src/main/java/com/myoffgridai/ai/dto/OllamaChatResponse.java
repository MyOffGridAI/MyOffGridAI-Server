package com.myoffgridai.ai.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

@JsonIgnoreProperties(ignoreUnknown = true)
public record OllamaChatResponse(
        OllamaMessage message,
        boolean done,
        @JsonProperty("total_duration") Long totalDuration,
        @JsonProperty("eval_count") Integer evalCount
) {
}
