package com.myoffgridai.sensors.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Positive;

/**
 * Request body for testing a serial port connection.
 *
 * @param portPath the serial port path to test
 * @param baudRate the baud rate to use for the test
 */
public record TestSensorRequest(
        @NotBlank String portPath,
        @Positive Integer baudRate
) {
}
