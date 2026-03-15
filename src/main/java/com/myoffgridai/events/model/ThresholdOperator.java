package com.myoffgridai.events.model;

/**
 * Comparison operators for sensor threshold events.
 *
 * <p>Used by {@link ScheduledEvent} to define the condition
 * that triggers a {@link EventType#SENSOR_THRESHOLD} event.</p>
 */
public enum ThresholdOperator {

    /** Triggers when the sensor value exceeds the threshold. */
    ABOVE,

    /** Triggers when the sensor value falls below the threshold. */
    BELOW,

    /** Triggers when the sensor value equals the threshold. */
    EQUALS
}
