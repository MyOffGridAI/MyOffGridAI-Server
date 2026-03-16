package com.myoffgridai.ai.dto;

/**
 * Classifies the type of content chunk emitted during a streaming inference response.
 *
 * <ul>
 *   <li>{@code THINKING} — reasoning trace content from {@code <think>} tags</li>
 *   <li>{@code CONTENT} — normal response content for the user</li>
 *   <li>{@code DONE} — terminal chunk carrying inference metadata</li>
 * </ul>
 */
public enum ChunkType {
    THINKING,
    CONTENT,
    DONE
}
