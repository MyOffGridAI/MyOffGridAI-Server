package com.myoffgridai.auth.dto;

import jakarta.validation.constraints.NotBlank;

/**
 * Request DTO for user authentication with username and password credentials.
 *
 * @author MyOffGridAI
 * @since 0.1.0
 */
public record LoginRequest(
        @NotBlank(message = "Username is required")
        String username,

        @NotBlank(message = "Password is required")
        String password
) {
}
