package com.myoffgridai.enrichment.dto;

import jakarta.validation.constraints.NotBlank;

/**
 * Request body for the fetch-url enrichment endpoint.
 *
 * @param url                 the URL to fetch
 * @param summarizeWithClaude whether to summarize content using Claude API
 */
public record FetchUrlRequest(
        @NotBlank String url,
        boolean summarizeWithClaude
) {
}
