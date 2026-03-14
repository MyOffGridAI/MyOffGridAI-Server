package com.myoffgridai.knowledge.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * Request DTO for updating a knowledge document's display name.
 *
 * @param displayName the new display name
 */
public record UpdateDisplayNameRequest(
        @NotBlank(message = "Display name must not be blank")
        @Size(max = 255, message = "Display name must not exceed 255 characters")
        String displayName
) {
}
