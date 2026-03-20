package com.myoffgridai.library.dto;

/**
 * Tracks the installation state of the kiwix-serve binary.
 *
 * <p>Used by {@link com.myoffgridai.library.service.KiwixProcessService}
 * to report whether kiwix-tools is present, being installed, or failed.</p>
 */
public enum KiwixInstallationStatus {
    NOT_CHECKED,
    CHECKING,
    NOT_INSTALLED,
    INSTALLING,
    INSTALLED,
    INSTALL_FAILED
}
