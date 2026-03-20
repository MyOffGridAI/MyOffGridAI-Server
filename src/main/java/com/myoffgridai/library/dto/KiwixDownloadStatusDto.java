package com.myoffgridai.library.dto;

/**
 * Progress status for a Kiwix ZIM file download.
 *
 * @param id                        the download identifier
 * @param filename                  the target filename
 * @param totalBytes                total expected file size
 * @param downloadedBytes           bytes downloaded so far
 * @param percentComplete           download completion percentage (0-100)
 * @param status                    the current download state
 * @param error                     error message if status is FAILED, null otherwise
 * @param speedBytesPerSecond       current download speed in bytes per second
 * @param estimatedSecondsRemaining estimated seconds until download completes
 */
public record KiwixDownloadStatusDto(
        String id,
        String filename,
        long totalBytes,
        long downloadedBytes,
        double percentComplete,
        KiwixDownloadState status,
        String error,
        double speedBytesPerSecond,
        long estimatedSecondsRemaining
) {
}
