package com.myoffgridai.auth.dto;

import com.myoffgridai.auth.model.Role;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.Size;

/**
 * Request DTO for updating a user's profile information.
 *
 * @author MyOffGridAI
 * @since 0.1.0
 */
public record UpdateUserRequest(
        @Size(min = 1, max = 100, message = "Display name must be between 1 and 100 characters")
        String displayName,

        @Email(message = "Email must be valid")
        String email,

        Role role
) {
}
