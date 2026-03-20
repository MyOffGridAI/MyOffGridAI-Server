package com.myoffgridai.ai.judge;

import jakarta.validation.constraints.NotBlank;

/**
 * Request body for the judge test endpoint.
 *
 * <p>Only the query is required. If response is omitted or blank,
 * the server generates one from the local LLM before evaluating.</p>
 *
 * @param query    the original user query
 * @param response optional assistant response to evaluate
 */
public record JudgeTestRequest(
        @NotBlank String query,
        String response
) {
}
