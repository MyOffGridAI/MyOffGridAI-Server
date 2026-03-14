package com.myoffgridai.sensors.dto;

import com.myoffgridai.sensors.model.DataFormat;
import com.myoffgridai.sensors.model.Sensor;
import com.myoffgridai.sensors.model.SensorType;

import java.time.Instant;
import java.util.UUID;

/**
 * Data transfer object for sensor metadata.
 *
 * @param id                  the sensor ID
 * @param name                the sensor display name
 * @param type                the sensor type
 * @param portPath            the serial port path
 * @param baudRate            the baud rate
 * @param dataFormat          the data format
 * @param valueField          the JSON field name for value extraction
 * @param unit                the measurement unit label
 * @param isActive            whether the sensor is actively polling
 * @param pollIntervalSeconds the polling interval in seconds
 * @param lowThreshold        the low threshold for alerts
 * @param highThreshold       the high threshold for alerts
 * @param createdAt           the creation timestamp
 * @param updatedAt           the last update timestamp
 */
public record SensorDto(
        UUID id,
        String name,
        SensorType type,
        String portPath,
        int baudRate,
        DataFormat dataFormat,
        String valueField,
        String unit,
        boolean isActive,
        int pollIntervalSeconds,
        Double lowThreshold,
        Double highThreshold,
        Instant createdAt,
        Instant updatedAt
) {

    /**
     * Converts a {@link Sensor} entity to a DTO.
     *
     * @param sensor the sensor entity
     * @return the sensor DTO
     */
    public static SensorDto from(Sensor sensor) {
        return new SensorDto(
                sensor.getId(),
                sensor.getName(),
                sensor.getType(),
                sensor.getPortPath(),
                sensor.getBaudRate(),
                sensor.getDataFormat(),
                sensor.getValueField(),
                sensor.getUnit(),
                sensor.getIsActive(),
                sensor.getPollIntervalSeconds(),
                sensor.getLowThreshold(),
                sensor.getHighThreshold(),
                sensor.getCreatedAt(),
                sensor.getUpdatedAt()
        );
    }
}
