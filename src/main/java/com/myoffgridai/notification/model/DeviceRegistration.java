package com.myoffgridai.notification.model;

import jakarta.persistence.*;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.Instant;
import java.util.UUID;

/**
 * Persistent entity representing a registered client device for MQTT push notifications.
 *
 * <p>Each Flutter app instance registers with the server, providing its device identifier
 * and MQTT client ID. The server uses this mapping to determine which MQTT topics
 * to publish notifications to for a given user.</p>
 *
 * <p>A unique constraint on {@code (userId, deviceId)} ensures one registration
 * per device per user — subsequent registrations update the existing record.</p>
 */
@Entity
@Table(name = "device_registrations",
        uniqueConstraints = @UniqueConstraint(
                name = "uk_device_registration_user_device",
                columnNames = {"user_id", "device_id"}),
        indexes = @Index(name = "idx_device_registration_user_id", columnList = "user_id"))
@EntityListeners(AuditingEntityListener.class)
public class DeviceRegistration {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "user_id", nullable = false)
    private UUID userId;

    @Column(name = "device_id", nullable = false)
    private String deviceId;

    @Column(name = "device_name")
    private String deviceName;

    @Column(nullable = false)
    private String platform;

    @Column(name = "mqtt_client_id")
    private String mqttClientId;

    @Column(name = "last_seen_at")
    private Instant lastSeenAt;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @Column(name = "updated_at")
    private Instant updatedAt;

    /**
     * Default constructor for JPA.
     */
    public DeviceRegistration() {
    }

    /**
     * Sets creation timestamp on first persist.
     */
    @PrePersist
    protected void onCreate() {
        if (createdAt == null) createdAt = Instant.now();
        updatedAt = Instant.now();
    }

    /**
     * Updates the modification timestamp on every update.
     */
    @PreUpdate
    protected void onUpdate() {
        updatedAt = Instant.now();
    }

    // ── Getters and Setters ─────────────────────────────────────────────────

    public UUID getId() { return id; }
    public void setId(UUID id) { this.id = id; }

    public UUID getUserId() { return userId; }
    public void setUserId(UUID userId) { this.userId = userId; }

    public String getDeviceId() { return deviceId; }
    public void setDeviceId(String deviceId) { this.deviceId = deviceId; }

    public String getDeviceName() { return deviceName; }
    public void setDeviceName(String deviceName) { this.deviceName = deviceName; }

    public String getPlatform() { return platform; }
    public void setPlatform(String platform) { this.platform = platform; }

    public String getMqttClientId() { return mqttClientId; }
    public void setMqttClientId(String mqttClientId) { this.mqttClientId = mqttClientId; }

    public Instant getLastSeenAt() { return lastSeenAt; }
    public void setLastSeenAt(Instant lastSeenAt) { this.lastSeenAt = lastSeenAt; }

    public Instant getCreatedAt() { return createdAt; }
    public void setCreatedAt(Instant createdAt) { this.createdAt = createdAt; }

    public Instant getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(Instant updatedAt) { this.updatedAt = updatedAt; }
}
