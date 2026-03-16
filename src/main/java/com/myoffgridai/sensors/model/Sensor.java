package com.myoffgridai.sensors.model;

import jakarta.persistence.*;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.Instant;
import java.util.UUID;

/**
 * Persistent entity representing a registered hardware sensor connected via serial port.
 *
 * @author MyOffGridAI
 * @since 0.1.0
 */
@Entity
@Table(name = "sensors", indexes = {
        @Index(name = "idx_sensor_user_id", columnList = "user_id"),
        @Index(name = "idx_sensor_port_path", columnList = "port_path", unique = true)
})
@EntityListeners(AuditingEntityListener.class)
public class Sensor {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "user_id", nullable = false)
    private UUID userId;

    @Column(nullable = false)
    private String name;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private SensorType type;

    @Column(name = "port_path", nullable = false)
    private String portPath;

    @Column(name = "baud_rate", nullable = false)
    private int baudRate = 9600;

    @Enumerated(EnumType.STRING)
    @Column(name = "data_format", nullable = false)
    private DataFormat dataFormat = DataFormat.CSV_LINE;

    @Column(name = "value_field")
    private String valueField;

    @Column
    private String unit;

    @Column(name = "is_active", nullable = false)
    private boolean isActive = false;

    @Column(name = "poll_interval_seconds", nullable = false)
    private int pollIntervalSeconds = 30;

    @Column(name = "low_threshold")
    private Double lowThreshold;

    @Column(name = "high_threshold")
    private Double highThreshold;

    @CreatedDate
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @LastModifiedDate
    @Column(name = "updated_at")
    private Instant updatedAt;

    public Sensor() {
    }

    @PrePersist
    protected void onCreate() {
        if (createdAt == null) createdAt = Instant.now();
        updatedAt = Instant.now();
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = Instant.now();
    }

    // ── Getters and Setters ─────────────────────────────────────────────────

    public UUID getId() { return id; }
    public void setId(UUID id) { this.id = id; }

    public UUID getUserId() { return userId; }
    public void setUserId(UUID userId) { this.userId = userId; }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public SensorType getType() { return type; }
    public void setType(SensorType type) { this.type = type; }

    public String getPortPath() { return portPath; }
    public void setPortPath(String portPath) { this.portPath = portPath; }

    public int getBaudRate() { return baudRate; }
    public void setBaudRate(int baudRate) { this.baudRate = baudRate; }

    public DataFormat getDataFormat() { return dataFormat; }
    public void setDataFormat(DataFormat dataFormat) { this.dataFormat = dataFormat; }

    public String getValueField() { return valueField; }
    public void setValueField(String valueField) { this.valueField = valueField; }

    public String getUnit() { return unit; }
    public void setUnit(String unit) { this.unit = unit; }

    public boolean getIsActive() { return isActive; }
    public void setIsActive(boolean active) { isActive = active; }

    public int getPollIntervalSeconds() { return pollIntervalSeconds; }
    public void setPollIntervalSeconds(int pollIntervalSeconds) { this.pollIntervalSeconds = pollIntervalSeconds; }

    public Double getLowThreshold() { return lowThreshold; }
    public void setLowThreshold(Double lowThreshold) { this.lowThreshold = lowThreshold; }

    public Double getHighThreshold() { return highThreshold; }
    public void setHighThreshold(Double highThreshold) { this.highThreshold = highThreshold; }

    public Instant getCreatedAt() { return createdAt; }
    public void setCreatedAt(Instant createdAt) { this.createdAt = createdAt; }

    public Instant getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(Instant updatedAt) { this.updatedAt = updatedAt; }
}
