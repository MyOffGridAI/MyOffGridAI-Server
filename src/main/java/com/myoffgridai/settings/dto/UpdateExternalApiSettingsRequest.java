package com.myoffgridai.settings.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Size;

/**
 * Request DTO for updating external API settings.
 *
 * <p>API key fields are nullable — a null value means "do not change the key".
 * An empty string means "clear the key".</p>
 *
 * @param anthropicApiKey     the Anthropic API key (null = no change, empty = clear)
 * @param anthropicModel      the Anthropic model to use
 * @param anthropicEnabled    whether to enable the Anthropic API
 * @param braveApiKey         the Brave Search API key (null = no change, empty = clear)
 * @param braveEnabled        whether to enable Brave Search
 * @param maxWebFetchSizeKb   max content size per URL fetch in KB (1–10240)
 * @param searchResultLimit   max results per search query (1–20)
 */
public record UpdateExternalApiSettingsRequest(
        String anthropicApiKey,
        @Size(max = 100) String anthropicModel,
        boolean anthropicEnabled,
        String braveApiKey,
        boolean braveEnabled,
        @Min(1) @Max(10240) int maxWebFetchSizeKb,
        @Min(1) @Max(20) int searchResultLimit
) {
}
