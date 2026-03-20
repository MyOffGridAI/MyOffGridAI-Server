package com.myoffgridai.library.dto;

/**
 * Lifecycle states for a Kiwix ZIM file download.
 */
public enum KiwixDownloadState {
    QUEUED,
    DOWNLOADING,
    COMPLETE,
    FAILED,
    CANCELLED
}
