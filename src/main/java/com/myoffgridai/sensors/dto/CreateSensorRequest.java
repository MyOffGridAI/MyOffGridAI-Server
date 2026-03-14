package com.myoffgridai.sensors.dto;

import com.myoffgridai.sensors.model.DataFormat;
import com.myoffgridai.sensors.model.SensorType;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

/**
 * Request body for registering a new sensor.
 *
 * @param name                the sensor display name
 * @param type                the sensor type
 * @param portPath            the serial port path (e.g. "/dev/ttyUSB0")
 * @param baudRate            the baud rate (default 9600)
 * @param dataFormat          the data format (default CSV_LINE)
 * @param valueField          the JSON field name for value extraction (JSON_LINE only)
 * @param unit                the measurement unit label
 * @param pollIntervalSeconds the polling interval in seconds
 * @param lowThreshold        optional low threshold for alerts
 * @param highThreshold       optional high threshold for alerts
 */
public record CreateSensorRequest(
        @NotBlank String name,
        @NotNull SensorType type,
        @NotBlank String portPath,
        Integer baudRate,
        DataFormat dataFormat,
        String valueField,
        String unit,
        @Min(5) @Max(3600) Integer pollIntervalSeconds,
        Double lowThreshold,
        Double highThreshold
) {
}
