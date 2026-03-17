package com.myoffgridai.ai.judge;

import jakarta.validation.constraints.NotBlank;

/**
 * Request body for the judge test endpoint.
 *
 * @param query    the original user query
 * @param response the assistant response to evaluate
 */
public record JudgeTestRequest(
        @NotBlank String query,
        @NotBlank String response
) {
}
