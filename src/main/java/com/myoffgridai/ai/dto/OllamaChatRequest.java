package com.myoffgridai.ai.dto;

import com.fasterxml.jackson.annotation.JsonInclude;

import java.util.List;
import java.util.Map;

@JsonInclude(JsonInclude.Include.NON_NULL)
public record OllamaChatRequest(
        String model,
        List<OllamaMessage> messages,
        boolean stream,
        Map<String, Object> options,
        Boolean think
) {
    /**
     * Convenience constructor without the think parameter (defaults to null / omitted).
     */
    public OllamaChatRequest(String model, List<OllamaMessage> messages,
                             boolean stream, Map<String, Object> options) {
        this(model, messages, stream, options, null);
    }
}
