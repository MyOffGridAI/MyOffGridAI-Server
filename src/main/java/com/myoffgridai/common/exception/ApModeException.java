package com.myoffgridai.common.exception;

/**
 * Thrown when an Access Point mode operation fails (e.g., hostapd/dnsmasq
 * start/stop, WiFi scanning, or network connection errors).
 */
public class ApModeException extends RuntimeException {

    public ApModeException(String message) {
        super(message);
    }

    public ApModeException(String message, Throwable cause) {
        super(message, cause);
    }
}
