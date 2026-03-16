package com.myoffgridai.ai.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * Request body for editing an existing message's content.
 *
 * @param content the updated message content
 */
public record EditMessageRequest(
        @NotBlank
        @Size(max = 32000)
        String content
) {}
