package com.myoffgridai.sensors.dto;

import com.myoffgridai.sensors.model.SensorReading;

import java.time.Instant;
import java.util.UUID;

/**
 * Data transfer object for a single sensor reading.
 *
 * @param id         the reading ID
 * @param sensorId   the associated sensor ID
 * @param value      the reading value
 * @param rawData    the raw data string from the sensor
 * @param recordedAt the timestamp of the reading
 */
public record SensorReadingDto(
        UUID id,
        UUID sensorId,
        double value,
        String rawData,
        Instant recordedAt
) {

    /**
     * Converts a {@link SensorReading} entity to a DTO.
     *
     * @param reading the sensor reading entity
     * @return the sensor reading DTO
     */
    public static SensorReadingDto from(SensorReading reading) {
        return new SensorReadingDto(
                reading.getId(),
                reading.getSensor().getId(),
                reading.getValue(),
                reading.getRawData(),
                reading.getRecordedAt()
        );
    }
}
