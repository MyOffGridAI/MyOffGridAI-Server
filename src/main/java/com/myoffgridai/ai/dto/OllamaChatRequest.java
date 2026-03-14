package com.myoffgridai.ai.dto;

import java.util.List;
import java.util.Map;

public record OllamaChatRequest(
        String model,
        List<OllamaMessage> messages,
        boolean stream,
        Map<String, Object> options
) {
}
