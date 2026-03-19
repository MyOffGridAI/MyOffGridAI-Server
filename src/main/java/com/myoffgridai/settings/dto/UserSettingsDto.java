package com.myoffgridai.settings.dto;

/**
 * Response DTO for user settings.
 *
 * @param themePreference the user's theme preference ({@code "light"}, {@code "dark"}, or {@code "system"})
 */
public record UserSettingsDto(
        String themePreference
) {
}
