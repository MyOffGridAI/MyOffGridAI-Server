package com.myoffgridai.sensors.model;

import jakarta.persistence.*;

import java.time.Instant;
import java.util.UUID;

/**
 * Persistent entity representing a single data reading from a hardware sensor.
 *
 * @author MyOffGridAI
 * @since 0.1.0
 */
@Entity
@Table(name = "sensor_readings", indexes = {
        @Index(name = "idx_sensor_reading_sensor_recorded",
                columnList = "sensor_id, recorded_at DESC")
})
public class SensorReading {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "sensor_id", nullable = false)
    private Sensor sensor;

    @Column(nullable = false)
    private double value;

    @Column(name = "raw_data")
    private String rawData;

    @Column(name = "recorded_at", nullable = false)
    private Instant recordedAt;

    public SensorReading() {
    }

    // ── Getters and Setters ─────────────────────────────────────────────────

    public UUID getId() { return id; }
    public void setId(UUID id) { this.id = id; }

    public Sensor getSensor() { return sensor; }
    public void setSensor(Sensor sensor) { this.sensor = sensor; }

    public double getValue() { return value; }
    public void setValue(double value) { this.value = value; }

    public String getRawData() { return rawData; }
    public void setRawData(String rawData) { this.rawData = rawData; }

    public Instant getRecordedAt() { return recordedAt; }
    public void setRecordedAt(Instant recordedAt) { this.recordedAt = recordedAt; }
}
