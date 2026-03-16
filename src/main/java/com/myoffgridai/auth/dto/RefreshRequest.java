package com.myoffgridai.auth.dto;

import jakarta.validation.constraints.NotBlank;

/**
 * Request DTO for refreshing an access token using a valid refresh token.
 *
 * @author MyOffGridAI
 * @since 0.1.0
 */
public record RefreshRequest(
        @NotBlank(message = "Refresh token is required")
        String refreshToken
) {
}
