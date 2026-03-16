package com.myoffgridai.auth.dto;

/**
 * Response DTO containing JWT token pair and authenticated user summary after login or registration.
 *
 * @author MyOffGridAI
 * @since 0.1.0
 */
public record AuthResponse(
        String accessToken,
        String refreshToken,
        String tokenType,
        long expiresIn,
        UserSummaryDto user
) {
}
