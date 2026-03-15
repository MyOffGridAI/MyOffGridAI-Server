package com.myoffgridai.events.model;

/**
 * Types of scheduled events.
 *
 * <p>Determines the trigger mechanism for a {@link ScheduledEvent}.</p>
 */
public enum EventType {

    /** Triggered by a cron expression on a fixed schedule. */
    SCHEDULED,

    /** Triggered when a sensor reading crosses a threshold. */
    SENSOR_THRESHOLD,

    /** Triggered at a recurring interval in minutes. */
    RECURRING
}
