package com.myoffgridai.sensors.dto;

/**
 * Request body for updating sensor threshold values.
 *
 * @param lowThreshold  the low threshold for alerts (null to clear)
 * @param highThreshold the high threshold for alerts (null to clear)
 */
public record UpdateThresholdsRequest(
        Double lowThreshold,
        Double highThreshold
) {
}
