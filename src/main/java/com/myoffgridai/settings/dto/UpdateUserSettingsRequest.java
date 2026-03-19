package com.myoffgridai.settings.dto;

import jakarta.validation.constraints.Pattern;

/**
 * Request DTO for updating user settings.
 *
 * @param themePreference the desired theme ({@code "light"}, {@code "dark"}, or {@code "system"})
 */
public record UpdateUserSettingsRequest(
        @Pattern(regexp = "light|dark|system", message = "themePreference must be light, dark, or system")
        String themePreference
) {
}
