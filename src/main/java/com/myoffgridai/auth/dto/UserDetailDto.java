package com.myoffgridai.auth.dto;

import com.myoffgridai.auth.model.Role;

import java.time.Instant;
import java.util.UUID;

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
