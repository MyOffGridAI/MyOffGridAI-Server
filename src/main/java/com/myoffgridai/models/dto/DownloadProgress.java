package com.myoffgridai.models.dto;

/**
 * Real-time progress state for a model download.
 *
 * @param downloadId               the unique download identifier
 * @param repoId                   the HuggingFace repository ID
 * @param filename                 the file being downloaded
 * @param status                   the current download status
 * @param bytesDownloaded          bytes downloaded so far
 * @param totalBytes               total file size in bytes
 * @param percentComplete          download completion percentage (0–100)
 * @param speedBytesPerSecond      current download speed in bytes/second
 * @param estimatedSecondsRemaining estimated time remaining in seconds
 * @param targetPath               the target file path on disk
 * @param errorMessage             error message if download failed (nullable)
 */
public record DownloadProgress(
        String downloadId,
        String repoId,
        String filename,
        DownloadStatus status,
        long bytesDownloaded,
        long totalBytes,
        double percentComplete,
        double speedBytesPerSecond,
        long estimatedSecondsRemaining,
        String targetPath,
        String errorMessage
) {
}
