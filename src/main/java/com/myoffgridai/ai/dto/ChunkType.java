package com.myoffgridai.ai.dto;

/**
 * Classifies the type of content chunk emitted during a streaming inference response.
 *
 * <ul>
 *   <li>{@code THINKING} — reasoning trace content from {@code <think>} tags</li>
 *   <li>{@code CONTENT} — normal response content for the user</li>
 *   <li>{@code DONE} — terminal chunk carrying inference metadata</li>
 *   <li>{@code JUDGE_EVALUATING} — judge is currently evaluating the local response</li>
 *   <li>{@code JUDGE_RESULT} — judge evaluation complete, content contains JSON score</li>
 *   <li>{@code ENHANCED_CONTENT} — tokens from the cloud frontier model</li>
 *   <li>{@code ENHANCED_DONE} — enhanced response stream complete</li>
 * </ul>
 */
public enum ChunkType {
    THINKING,
    CONTENT,
    DONE,

    /** Judge is currently evaluating the local response. */
    JUDGE_EVALUATING,

    /** Judge evaluation complete. Content contains JSON: {"score":X,"reason":"...","needsCloud":true/false} */
    JUDGE_RESULT,

    /** Tokens from the cloud frontier model (enhanced response). */
    ENHANCED_CONTENT,

    /** Enhanced response stream complete. */
    ENHANCED_DONE
}
