package com.myoffgridai.ai.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

@JsonIgnoreProperties(ignoreUnknown = true)
public record OllamaChatChunk(
        OllamaMessage message,
        boolean done
) {
}
