package com.myoffgridai.ai.dto;

/**
 * A single chunk emitted during a streaming inference response with thinking support.
 *
 * <p>Each chunk is classified by its {@link ChunkType}:
 * <ul>
 *   <li>{@code THINKING} — reasoning trace text (from {@code <think>} tags)</li>
 *   <li>{@code CONTENT} — normal response text for the user</li>
 *   <li>{@code DONE} — terminal chunk; {@code text} is null, {@code metadata} is populated</li>
 * </ul>
 *
 * @param type     the classification of this chunk
 * @param text     the text content (null on DONE chunks)
 * @param metadata inference performance metadata (only set on DONE chunks)
 */
public record InferenceChunk(
        ChunkType type,
        String text,
        InferenceMetadata metadata
) {}
