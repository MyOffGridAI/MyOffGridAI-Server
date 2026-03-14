package com.myoffgridai.system.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record InitializeRequest(
        @NotBlank(message = "Instance name is required")
        @Size(min = 1, max = 100, message = "Instance name must be between 1 and 100 characters")
        String instanceName,

        @NotBlank(message = "Username is required")
        @Size(min = 3, max = 50, message = "Username must be between 3 and 50 characters")
        String username,

        @NotBlank(message = "Display name is required")
        String displayName,

        String email,

        @NotBlank(message = "Password is required")
        String password
) {}
