package com.myoffgridai.ai.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * Request body for sending a message within a conversation.
 *
 * @author MyOffGridAI
 * @since 0.1.0
 */
public record SendMessageRequest(
        @NotBlank
        @Size(max = 32000)
        String content,
        boolean stream
) {
}
