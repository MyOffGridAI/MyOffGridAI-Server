package com.myoffgridai.auth.dto;

import com.myoffgridai.auth.model.Role;

import java.time.Instant;
import java.util.UUID;

/**
 * Detailed DTO containing all user profile fields including timestamps.
 *
 * @author MyOffGridAI
 * @since 0.1.0
 */
public record UserDetailDto(
        UUID id,
        String username,
        String email,
        String displayName,
        Role role,
        boolean isActive,
        Instant createdAt,
        Instant updatedAt,
        Instant lastLoginAt
) {
}
