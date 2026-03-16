package com.myoffgridai.enrichment.dto;

/**
 * Status of the enrichment subsystem.
 *
 * @param claudeAvailable    whether the Claude API is configured and enabled
 * @param braveAvailable     whether the Brave Search API is configured and enabled
 * @param maxWebFetchSizeKb  the max content size per URL fetch in KB
 * @param searchResultLimit  the max results per search query
 */
public record EnrichmentStatusDto(
        boolean claudeAvailable,
        boolean braveAvailable,
        int maxWebFetchSizeKb,
        int searchResultLimit
) {
}
