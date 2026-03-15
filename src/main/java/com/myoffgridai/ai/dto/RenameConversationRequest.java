package com.myoffgridai.ai.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * Request body for renaming a conversation.
 *
 * @param title the new title (required, max 100 characters)
 */
public record RenameConversationRequest(
        @NotBlank @Size(max = 100) String title
) {
}
