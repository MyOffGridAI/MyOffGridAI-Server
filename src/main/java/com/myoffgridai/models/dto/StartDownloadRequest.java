package com.myoffgridai.models.dto;

import jakarta.validation.constraints.NotBlank;

/**
 * Request body for initiating a model download.
 *
 * @param repoId   the HuggingFace repository ID (e.g. "author/model-name")
 * @param filename the specific file to download (e.g. "model-Q4_K_M.gguf")
 */
public record StartDownloadRequest(
        @NotBlank String repoId,
        @NotBlank String filename
) {
}
