package com.myoffgridai.ai.dto;

import java.time.Instant;

/**
 * Provider-agnostic model information returned by {@link com.myoffgridai.ai.service.InferenceService}.
 *
 * <p>Normalises model metadata from different inference providers (Ollama, LM Studio)
 * into a single shape that the REST API and client consume.
 *
 * @param id         model identifier string (e.g. "llama3:8b" or "Jackrong/Qwen3.5-...")
 * @param name       human-readable display name
 * @param sizeBytes  model file size in bytes (nullable — not all providers report this)
 * @param format     model format such as "gguf" or "mlx" (nullable)
 * @param modifiedAt last-modified timestamp (nullable)
 */
public record InferenceModelInfo(
        String id,
        String name,
        Long sizeBytes,
        String format,
        Instant modifiedAt
) {}
