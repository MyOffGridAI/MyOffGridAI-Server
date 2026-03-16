package com.myoffgridai.models.dto;

/**
 * Status of a model file download.
 */
public enum DownloadStatus {
    /** Download is queued but not yet started. */
    QUEUED,
    /** Download is actively in progress. */
    DOWNLOADING,
    /** Download is paused (reserved for future use). */
    PAUSED,
    /** Download completed successfully. */
    COMPLETED,
    /** Download failed with an error. */
    FAILED,
    /** Download was cancelled by the user. */
    CANCELLED
}
