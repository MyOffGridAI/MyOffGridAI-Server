package com.myoffgridai.models.dto;

import com.fasterxml.jackson.annotation.JsonInclude;

/**
 * A single file available in a HuggingFace model repository.
 *
 * <p>When enriched by {@link com.myoffgridai.models.service.QuantizationRecommendationService},
 * the quantization metadata fields are populated. Unenriched files have null quantization
 * fields which are omitted from JSON serialization via {@link JsonInclude}.</p>
 *
 * @param rfilename        the relative filename (e.g. "model-Q4_K_M.gguf")
 * @param size             file size in bytes (nullable — not all repos report size)
 * @param blobId           the blob hash for download URL construction (nullable)
 * @param quantizationType the parsed GGUF quantization type (nullable)
 * @param qualityLabel     human-readable quality description (nullable)
 * @param qualityRank      integer quality rank, higher is better (nullable)
 * @param estimatedRamBytes estimated RAM required to load this model (nullable)
 * @param recommended      true if this is the recommended variant for the user's system (nullable)
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public record HfModelFileDto(
        String rfilename,
        Long size,
        String blobId,
        QuantizationType quantizationType,
        String qualityLabel,
        Integer qualityRank,
        Long estimatedRamBytes,
        Boolean recommended
) {

    /**
     * Backward-compatible constructor for creating file entries without quantization metadata.
     *
     * @param rfilename the relative filename
     * @param size      file size in bytes (nullable)
     * @param blobId    the blob hash (nullable)
     */
    public HfModelFileDto(String rfilename, Long size, String blobId) {
        this(rfilename, size, blobId, null, null, null, null, null);
    }

    /**
     * Returns a copy of this file DTO with the recommended flag set.
     *
     * @param recommended whether this variant is recommended
     * @return a new HfModelFileDto with the recommended flag applied
     */
    public HfModelFileDto withRecommended(boolean recommended) {
        return new HfModelFileDto(rfilename, size, blobId, quantizationType,
                qualityLabel, qualityRank, estimatedRamBytes, recommended);
    }
}
