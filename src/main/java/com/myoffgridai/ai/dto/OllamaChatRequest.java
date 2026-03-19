package com.myoffgridai.ai.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.List;
import java.util.Map;

/**
 * Request object sent to the Ollama chat API containing the model, messages, and options.
 *
 * @author MyOffGridAI
 * @since 0.1.0
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public record OllamaChatRequest(
        String model,
        List<OllamaMessage> messages,
        boolean stream,
        Map<String, Object> options,
        Boolean think,
        @JsonProperty("keep_alive") String keepAlive
) {
    /**
     * Convenience constructor without think or keep_alive (defaults to null / omitted).
     */
    public OllamaChatRequest(String model, List<OllamaMessage> messages,
                             boolean stream, Map<String, Object> options) {
        this(model, messages, stream, options, null, null);
    }

    /**
     * Convenience constructor without keep_alive (defaults to null / omitted).
     */
    public OllamaChatRequest(String model, List<OllamaMessage> messages,
                             boolean stream, Map<String, Object> options, Boolean think) {
        this(model, messages, stream, options, think, null);
    }
}
