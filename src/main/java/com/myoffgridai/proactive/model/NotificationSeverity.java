package com.myoffgridai.proactive.model;

/**
 * Severity levels for notifications, used by the MQTT push pipeline
 * and displayed in the Flutter client UI.
 */
public enum NotificationSeverity {
    /** Informational notification — no action required. */
    INFO,
    /** Warning notification — attention recommended. */
    WARNING,
    /** Critical notification — immediate action required. */
    CRITICAL
}
