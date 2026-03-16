package com.myoffgridai.auth.dto;

import jakarta.validation.constraints.NotBlank;

/**
 * Request DTO for changing a user's password.
 *
 * @author MyOffGridAI
 * @since 0.1.0
 */
public record ChangePasswordRequest(
        @NotBlank(message = "Current password is required")
        String currentPassword,

        @NotBlank(message = "New password is required")
        String newPassword
) {
}
