package com.myoffgridai.ai.dto;

import java.time.Instant;

/**
 * Data transfer object representing metadata about an available Ollama model.
 *
 * @author MyOffGridAI
 * @since 0.1.0
 */
public record OllamaModelInfo(
        String name,
        long size,
        Instant modifiedAt
) {
}
