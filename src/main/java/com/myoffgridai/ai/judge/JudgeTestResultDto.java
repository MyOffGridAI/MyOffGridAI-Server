package com.myoffgridai.ai.judge;

/**
 * Result of a manual judge test invocation.
 *
 * @param score          quality score from 1 to 10 (0.0 if unavailable)
 * @param reason         brief explanation of the score (null if unavailable)
 * @param needsCloud     whether the judge recommends cloud refinement
 * @param judgeAvailable whether the judge was available to perform the evaluation
 * @param error          error message if the evaluation failed (null on success)
 */
public record JudgeTestResultDto(
        double score,
        String reason,
        boolean needsCloud,
        boolean judgeAvailable,
        String error
) {
}
