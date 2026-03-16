package com.myoffgridai.auth.dto;

import com.myoffgridai.auth.model.Role;

import java.util.UUID;

/**
 * Lightweight DTO containing essential user fields for list views and embedded references.
 *
 * @author MyOffGridAI
 * @since 0.1.0
 */
public record UserSummaryDto(
        UUID id,
        String username,
        String displayName,
        Role role,
        boolean isActive
) {
}
