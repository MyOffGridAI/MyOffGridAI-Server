package com.myoffgridai.models.dto;

import java.time.Instant;

/**
 * A model file already downloaded to the LM Studio models directory.
 *
 * @param filename          the file name (e.g. "model-Q4_K_M.gguf")
 * @param repoId            the HuggingFace repo ID derived from directory path (nullable)
 * @param format            the file format ("gguf", "mlx", or "unknown")
 * @param sizeBytes         the file size in bytes
 * @param lastModified      the last modification time
 * @param isCurrentlyLoaded whether this model matches the active inference model
 */
public record LocalModelFileDto(
        String filename,
        String repoId,
        String format,
        long sizeBytes,
        Instant lastModified,
        boolean isCurrentlyLoaded
) {
}
