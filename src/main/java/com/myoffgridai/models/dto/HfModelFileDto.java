package com.myoffgridai.models.dto;

/**
 * A single file available in a HuggingFace model repository.
 *
 * @param rfilename the relative filename (e.g. "model-Q4_K_M.gguf")
 * @param size      file size in bytes (nullable — not all repos report size)
 * @param blobId    the blob hash for download URL construction (nullable)
 */
public record HfModelFileDto(
        String rfilename,
        Long size,
        String blobId
) {
}
