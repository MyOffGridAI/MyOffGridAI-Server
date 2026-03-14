package com.myoffgridai.privacy.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record ExportRequest(
        @NotBlank(message = "Passphrase is required")
        @Size(min = 8, message = "Passphrase must be at least 8 characters")
        String passphrase
) {}
