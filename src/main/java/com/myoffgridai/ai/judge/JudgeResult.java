package com.myoffgridai.ai.judge;

/**
 * Holds the parsed evaluation result from the AI judge model.
 *
 * @param score      quality score from 1 (poor) to 10 (excellent)
 * @param reason     brief explanation of the score
 * @param needsCloud whether the judge recommends cloud refinement
 */
public record JudgeResult(double score, String reason, boolean needsCloud) {
}
