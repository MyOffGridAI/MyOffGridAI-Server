package com.myoffgridai.settings.model;

import com.myoffgridai.common.util.AesAttributeConverter;
import jakarta.persistence.*;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.Instant;
import java.util.UUID;

/**
 * Singleton entity storing external API configuration (Anthropic Claude,
 * Brave Search). API keys are stored AES-256-GCM encrypted at rest.
 *
 * <p>Only one row exists in this table, enforced by the unique
 * {@link #singletonGuard} column.</p>
 */
@Entity
@Table(name = "external_api_settings")
@EntityListeners(AuditingEntityListener.class)
public class ExternalApiSettings {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "singleton_guard", unique = true, nullable = false)
    private String singletonGuard = "SINGLETON";

    @Convert(converter = AesAttributeConverter.class)
    @Column(name = "anthropic_api_key")
    private String anthropicApiKey;

    @Column(name = "anthropic_model", nullable = false)
    private String anthropicModel = "claude-sonnet-4-20250514";

    @Column(name = "anthropic_enabled", nullable = false)
    private boolean anthropicEnabled = false;

    @Convert(converter = AesAttributeConverter.class)
    @Column(name = "brave_api_key")
    private String braveApiKey;

    @Column(name = "brave_enabled", nullable = false)
    private boolean braveEnabled = false;

    @Column(name = "max_web_fetch_size_kb", nullable = false)
    private int maxWebFetchSizeKb = 512;

    @Column(name = "search_result_limit", nullable = false)
    private int searchResultLimit = 5;

    @CreatedDate
    @Column(name = "created_at", updatable = false)
    private Instant createdAt;

    @LastModifiedDate
    @Column(name = "updated_at")
    private Instant updatedAt;

    @PrePersist
    protected void onPrePersist() {
        if (createdAt == null) createdAt = Instant.now();
        if (updatedAt == null) updatedAt = Instant.now();
    }

    @PreUpdate
    protected void onPreUpdate() {
        updatedAt = Instant.now();
    }

    public UUID getId() { return id; }
    public void setId(UUID id) { this.id = id; }

    public String getSingletonGuard() { return singletonGuard; }

    public String getAnthropicApiKey() { return anthropicApiKey; }
    public void setAnthropicApiKey(String anthropicApiKey) { this.anthropicApiKey = anthropicApiKey; }

    public String getAnthropicModel() { return anthropicModel; }
    public void setAnthropicModel(String anthropicModel) { this.anthropicModel = anthropicModel != null ? anthropicModel : "claude-sonnet-4-20250514"; }

    public boolean isAnthropicEnabled() { return anthropicEnabled; }
    public void setAnthropicEnabled(boolean anthropicEnabled) { this.anthropicEnabled = anthropicEnabled; }

    public String getBraveApiKey() { return braveApiKey; }
    public void setBraveApiKey(String braveApiKey) { this.braveApiKey = braveApiKey; }

    public boolean isBraveEnabled() { return braveEnabled; }
    public void setBraveEnabled(boolean braveEnabled) { this.braveEnabled = braveEnabled; }

    public int getMaxWebFetchSizeKb() { return maxWebFetchSizeKb; }
    public void setMaxWebFetchSizeKb(int maxWebFetchSizeKb) { this.maxWebFetchSizeKb = maxWebFetchSizeKb > 0 ? maxWebFetchSizeKb : 512; }

    public int getSearchResultLimit() { return searchResultLimit; }
    public void setSearchResultLimit(int searchResultLimit) { this.searchResultLimit = searchResultLimit > 0 ? searchResultLimit : 5; }

    public Instant getCreatedAt() { return createdAt; }
    public Instant getUpdatedAt() { return updatedAt; }
}
