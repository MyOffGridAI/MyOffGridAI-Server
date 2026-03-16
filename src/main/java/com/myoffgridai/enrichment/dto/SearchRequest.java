package com.myoffgridai.enrichment.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;

/**
 * Request body for the web search enrichment endpoint.
 *
 * @param query               the search query
 * @param storeTopN           how many top results to store in Knowledge Base (0 = don't store)
 * @param summarizeWithClaude whether to summarize fetched content using Claude API
 */
public record SearchRequest(
        @NotBlank String query,
        @Min(0) @Max(10) int storeTopN,
        boolean summarizeWithClaude
) {
}
