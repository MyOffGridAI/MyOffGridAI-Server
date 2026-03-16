package com.myoffgridai.settings.dto;

/**
 * Response DTO for external API settings. Never contains actual key values.
 *
 * @param anthropicEnabled       whether the Anthropic API is enabled
 * @param anthropicModel         the configured Anthropic model
 * @param anthropicKeyConfigured whether an Anthropic API key has been set
 * @param braveEnabled           whether the Brave Search API is enabled
 * @param braveKeyConfigured     whether a Brave Search API key has been set
 * @param maxWebFetchSizeKb      max content size per URL fetch in KB
 * @param searchResultLimit      max results per search query
 */
public record ExternalApiSettingsDto(
        boolean anthropicEnabled,
        String anthropicModel,
        boolean anthropicKeyConfigured,
        boolean braveEnabled,
        boolean braveKeyConfigured,
        int maxWebFetchSizeKb,
        int searchResultLimit
) {
}
