package com.myoffgridai.models.dto;

/**
 * Response returned when a model download is successfully initiated.
 *
 * @param downloadId        the unique download identifier for progress tracking
 * @param targetPath        the target path on disk where the file will be saved
 * @param estimatedSizeBytes the estimated file size from HuggingFace metadata (nullable)
 */
public record DownloadStartedDto(
        String downloadId,
        String targetPath,
        Long estimatedSizeBytes
) {
}
