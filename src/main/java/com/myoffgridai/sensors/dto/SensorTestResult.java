package com.myoffgridai.sensors.dto;

/**
 * Result of a sensor connection test.
 *
 * @param success    whether the connection test succeeded
 * @param portPath   the port path that was tested
 * @param baudRate   the baud rate used
 * @param sampleData a raw sample line received, or null if none
 * @param message    a human-readable status message
 */
public record SensorTestResult(
        boolean success,
        String portPath,
        int baudRate,
        String sampleData,
        String message
) {
}
