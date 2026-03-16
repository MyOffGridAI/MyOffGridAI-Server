package com.myoffgridai.ai.dto;

/**
 * Performance metadata collected during an inference request.
 *
 * <p>Attached to the terminal {@link ChunkType#DONE} chunk of a streaming
 * inference response so the client can display generation statistics.
 *
 * @param tokensGenerated  total completion tokens produced
 * @param tokensPerSecond  generation speed (completion tokens / wall-clock seconds)
 * @param inferenceTimeSeconds  wall-clock inference duration in seconds
 * @param stopReason  reason the model stopped generating (e.g. "stop", "length")
 */
public record InferenceMetadata(
        int tokensGenerated,
        double tokensPerSecond,
        double inferenceTimeSeconds,
        String stopReason
) {}
