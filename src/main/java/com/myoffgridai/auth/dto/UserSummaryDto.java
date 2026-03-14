package com.myoffgridai.auth.dto;

import com.myoffgridai.auth.model.Role;

import java.util.UUID;

public record UserSummaryDto(
        UUID id,
        String username,
        String displayName,
        Role role,
        boolean isActive
) {
}
