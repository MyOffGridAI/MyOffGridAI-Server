package com.myoffgridai.system.model;

import jakarta.persistence.*;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "system_config")
@EntityListeners(AuditingEntityListener.class)
public class SystemConfig {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(nullable = false)
    private boolean initialized = false;

    @Column(name = "instance_name")
    private String instanceName;

    @Column(name = "fortress_enabled", nullable = false)
    private boolean fortressEnabled = false;

    @Column(name = "fortress_enabled_at")
    private Instant fortressEnabledAt;

    @Column(name = "fortress_enabled_by_user_id")
    private UUID fortressEnabledByUserId;

    @Column(name = "ap_mode_enabled", nullable = false)
    private boolean apModeEnabled = false;

    @Column(name = "wifi_configured", nullable = false)
    private boolean wifiConfigured = false;

    @CreatedDate
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @LastModifiedDate
    @Column(name = "updated_at")
    private Instant updatedAt;

    public SystemConfig() {
    }

    // ── Getters and Setters ─────────────────────────────────────────────────

    public UUID getId() { return id; }
    public void setId(UUID id) { this.id = id; }

    public boolean isInitialized() { return initialized; }
    public void setInitialized(boolean initialized) { this.initialized = initialized; }

    public String getInstanceName() { return instanceName; }
    public void setInstanceName(String instanceName) { this.instanceName = instanceName; }

    public boolean isFortressEnabled() { return fortressEnabled; }
    public void setFortressEnabled(boolean fortressEnabled) { this.fortressEnabled = fortressEnabled; }

    public Instant getFortressEnabledAt() { return fortressEnabledAt; }
    public void setFortressEnabledAt(Instant fortressEnabledAt) { this.fortressEnabledAt = fortressEnabledAt; }

    public UUID getFortressEnabledByUserId() { return fortressEnabledByUserId; }
    public void setFortressEnabledByUserId(UUID fortressEnabledByUserId) { this.fortressEnabledByUserId = fortressEnabledByUserId; }

    public boolean isApModeEnabled() { return apModeEnabled; }
    public void setApModeEnabled(boolean apModeEnabled) { this.apModeEnabled = apModeEnabled; }

    public boolean isWifiConfigured() { return wifiConfigured; }
    public void setWifiConfigured(boolean wifiConfigured) { this.wifiConfigured = wifiConfigured; }

    public Instant getCreatedAt() { return createdAt; }
    public void setCreatedAt(Instant createdAt) { this.createdAt = createdAt; }

    public Instant getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(Instant updatedAt) { this.updatedAt = updatedAt; }
}
