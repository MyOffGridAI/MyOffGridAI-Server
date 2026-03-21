package com.myoffgridai.knowledge.dto;

import jakarta.validation.constraints.NotNull;

/**
 * Request DTO for updating the sharing status of a knowledge document.
 *
 * @param shared whether the document should be shared with all users
 */
public record UpdateSharingRequest(@NotNull Boolean shared) {
}
