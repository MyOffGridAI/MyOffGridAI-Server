package com.myoffgridai.events.model;

import jakarta.persistence.*;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.Instant;
import java.util.UUID;

/**
 * Persistent entity representing a user-defined scheduled event.
 *
 * <p>Events can be cron-scheduled, sensor-threshold-based, or recurring
 * at a fixed interval. When triggered, they execute an action such as
 * sending a push notification or running an AI prompt.</p>
 */
@Entity
@Table(name = "scheduled_events", indexes = {
        @Index(name = "idx_event_user_id", columnList = "user_id"),
        @Index(name = "idx_event_enabled_type", columnList = "is_enabled, event_type")
})
@EntityListeners(AuditingEntityListener.class)
public class ScheduledEvent {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "user_id", nullable = false)
    private UUID userId;

    @Column(nullable = false)
    private String name;

    @Column(columnDefinition = "TEXT")
    private String description;

    @Enumerated(EnumType.STRING)
    @Column(name = "event_type", nullable = false)
    private EventType eventType;

    @Column(name = "is_enabled", nullable = false)
    private boolean isEnabled = true;

    @Column(name = "cron_expression")
    private String cronExpression;

    @Column(name = "recurring_interval_minutes")
    private Integer recurringIntervalMinutes;

    @Column(name = "sensor_id")
    private UUID sensorId;

    @Enumerated(EnumType.STRING)
    @Column(name = "threshold_operator")
    private ThresholdOperator thresholdOperator;

    @Column(name = "threshold_value")
    private Double thresholdValue;

    @Enumerated(EnumType.STRING)
    @Column(name = "action_type", nullable = false)
    private ActionType actionType;

    @Column(name = "action_payload", nullable = false, columnDefinition = "TEXT")
    private String actionPayload;

    @Column(name = "last_triggered_at")
    private Instant lastTriggeredAt;

    @Column(name = "next_fire_at")
    private Instant nextFireAt;

    @CreatedDate
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @LastModifiedDate
    @Column(name = "updated_at")
    private Instant updatedAt;

    public ScheduledEvent() {
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

    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }

    public EventType getEventType() { return eventType; }
    public void setEventType(EventType eventType) { this.eventType = eventType; }

    public boolean getIsEnabled() { return isEnabled; }
    public void setIsEnabled(boolean enabled) { isEnabled = enabled; }

    public String getCronExpression() { return cronExpression; }
    public void setCronExpression(String cronExpression) { this.cronExpression = cronExpression; }

    public Integer getRecurringIntervalMinutes() { return recurringIntervalMinutes; }
    public void setRecurringIntervalMinutes(Integer recurringIntervalMinutes) {
        this.recurringIntervalMinutes = recurringIntervalMinutes;
    }

    public UUID getSensorId() { return sensorId; }
    public void setSensorId(UUID sensorId) { this.sensorId = sensorId; }

    public ThresholdOperator getThresholdOperator() { return thresholdOperator; }
    public void setThresholdOperator(ThresholdOperator thresholdOperator) {
        this.thresholdOperator = thresholdOperator;
    }

    public Double getThresholdValue() { return thresholdValue; }
    public void setThresholdValue(Double thresholdValue) { this.thresholdValue = thresholdValue; }

    public ActionType getActionType() { return actionType; }
    public void setActionType(ActionType actionType) { this.actionType = actionType; }

    public String getActionPayload() { return actionPayload; }
    public void setActionPayload(String actionPayload) { this.actionPayload = actionPayload; }

    public Instant getLastTriggeredAt() { return lastTriggeredAt; }
    public void setLastTriggeredAt(Instant lastTriggeredAt) { this.lastTriggeredAt = lastTriggeredAt; }

    public Instant getNextFireAt() { return nextFireAt; }
    public void setNextFireAt(Instant nextFireAt) { this.nextFireAt = nextFireAt; }

    public Instant getCreatedAt() { return createdAt; }
    public void setCreatedAt(Instant createdAt) { this.createdAt = createdAt; }

    public Instant getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(Instant updatedAt) { this.updatedAt = updatedAt; }
}
