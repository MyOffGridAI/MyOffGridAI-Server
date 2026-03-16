package com.myoffgridai.ai.dto;

/**
 * Data transfer object representing the currently active LLM and embedding model names.
 *
 * @author MyOffGridAI
 * @since 0.1.0
 */
public record ActiveModelDto(
        String modelName,
        String embedModelName
) {
}
