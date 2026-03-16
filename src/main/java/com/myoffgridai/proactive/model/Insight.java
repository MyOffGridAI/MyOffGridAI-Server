package com.myoffgridai.proactive.model;

import jakarta.persistence.*;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.Instant;
import java.util.UUID;

/**
 * Persistent entity representing an AI-generated insight based on user activity patterns.
 *
 * @author MyOffGridAI
 * @since 0.1.0
 */
@Entity
@Table(name = "insights", indexes = {
        @Index(name = "idx_insight_user_id", columnList = "user_id")
})
@EntityListeners(AuditingEntityListener.class)
public class Insight {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "user_id", nullable = false)
    private UUID userId;

    @Column(nullable = false, columnDefinition = "TEXT")
    private String content;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private InsightCategory category;

    @Column(name = "is_read", nullable = false)
    private boolean isRead = false;

    @Column(name = "is_dismissed", nullable = false)
    private boolean isDismissed = false;

    @Column(name = "generated_at", nullable = false, updatable = false)
    private Instant generatedAt;

    @Column(name = "read_at")
    private Instant readAt;

    public Insight() {
    }

    @PrePersist
    protected void onCreate() {
        if (generatedAt == null) generatedAt = Instant.now();
    }

    // ── Getters and Setters ─────────────────────────────────────────────────

    public UUID getId() { return id; }
    public void setId(UUID id) { this.id = id; }

    public UUID getUserId() { return userId; }
    public void setUserId(UUID userId) { this.userId = userId; }

    public String getContent() { return content; }
    public void setContent(String content) { this.content = content; }

    public InsightCategory getCategory() { return category; }
    public void setCategory(InsightCategory category) { this.category = category; }

    public boolean getIsRead() { return isRead; }
    public void setIsRead(boolean read) { isRead = read; }

    public boolean getIsDismissed() { return isDismissed; }
    public void setIsDismissed(boolean dismissed) { isDismissed = dismissed; }

    public Instant getGeneratedAt() { return generatedAt; }
    public void setGeneratedAt(Instant generatedAt) { this.generatedAt = generatedAt; }

    public Instant getReadAt() { return readAt; }
    public void setReadAt(Instant readAt) { this.readAt = readAt; }
}
