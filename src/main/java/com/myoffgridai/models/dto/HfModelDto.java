package com.myoffgridai.models.dto;

import java.time.Instant;
import java.util.List;

/**
 * Metadata for a HuggingFace model repository.
 *
 * @param id           the full repository ID (e.g. "TheBloke/Llama-2-7B-GGUF")
 * @param modelId      just the model name portion
 * @param author       the repository author/organization
 * @param downloads    total download count
 * @param likes        total like count
 * @param tags         model tags (e.g. "text-generation", "gguf")
 * @param pipelineTag  the pipeline type (e.g. "text-generation")
 * @param gated        whether the model requires authorization to access
 * @param lastModified the last modified timestamp
 * @param siblings     available files in the repository
 */
public record HfModelDto(
        String id,
        String modelId,
        String author,
        long downloads,
        long likes,
        List<String> tags,
        String pipelineTag,
        boolean gated,
        Instant lastModified,
        List<HfModelFileDto> siblings
) {
}
