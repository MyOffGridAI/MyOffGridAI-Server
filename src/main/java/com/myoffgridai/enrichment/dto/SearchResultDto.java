package com.myoffgridai.enrichment.dto;

/**
 * A single web search result from the Brave Search API.
 *
 * @param title         the result title
 * @param url           the result URL
 * @param description   the result snippet/description
 * @param publishedDate the publication date (may be null)
 */
public record SearchResultDto(
        String title,
        String url,
        String description,
        String publishedDate
) {
}
