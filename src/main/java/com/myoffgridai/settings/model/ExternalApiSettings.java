package com.myoffgridai.settings.model;

import com.myoffgridai.common.util.AesAttributeConverter;
import com.myoffgridai.config.AppConstants;
import com.myoffgridai.frontier.FrontierProvider;
import jakarta.persistence.*;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.Instant;
import java.util.UUID;

/**
 * Singleton entity storing external API configuration (Anthropic Claude,
 * Brave Search, HuggingFace). API keys are stored AES-256-GCM encrypted at rest.
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

    @Convert(converter = AesAttributeConverter.class)
    @Column(name = "hugging_face_token")
    private String huggingFaceToken;

    @Column(name = "hugging_face_enabled", nullable = false)
    private boolean huggingFaceEnabled = false;

    /** Grok (xAI) API key, encrypted at rest. */
    @Convert(converter = AesAttributeConverter.class)
    @Column(name = "grok_api_key", length = 1000)
    private String grokApiKey;

    /** Whether the Grok (xAI) frontier provider is enabled. */
    @Column(name = "grok_enabled", nullable = false)
    private boolean grokEnabled = false;

    /** OpenAI API key, encrypted at rest. */
    @Convert(converter = AesAttributeConverter.class)
    @Column(name = "openai_api_key", length = 1000)
    private String openAiApiKey;

    /** Whether the OpenAI frontier provider is enabled. */
    @Column(name = "openai_enabled", nullable = false)
    private boolean openAiEnabled = false;

    /** Preferred frontier provider for cloud refinement routing. */
    @Enumerated(EnumType.STRING)
    @Column(name = "preferred_frontier_provider", length = 20)
    private FrontierProvider preferredFrontierProvider = FrontierProvider.CLAUDE;

    /** Whether the AI judge evaluation pipeline is enabled. */
    @Column(name = "judge_enabled", nullable = false)
    private boolean judgeEnabled = false;

    /** Filename of the judge GGUF model in the models directory. */
    @Column(name = "judge_model_filename", length = 500)
    private String judgeModelFilename;

    /** Minimum judge score (1–10) below which cloud refinement is triggered. */
    @Column(name = "judge_score_threshold", nullable = false)
    private double judgeScoreThreshold = AppConstants.JUDGE_DEFAULT_SCORE_THRESHOLD;

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

    public String getHuggingFaceToken() { return huggingFaceToken; }
    public void setHuggingFaceToken(String huggingFaceToken) { this.huggingFaceToken = huggingFaceToken; }

    public boolean isHuggingFaceEnabled() { return huggingFaceEnabled; }
    public void setHuggingFaceEnabled(boolean huggingFaceEnabled) { this.huggingFaceEnabled = huggingFaceEnabled; }

    public String getGrokApiKey() { return grokApiKey; }
    public void setGrokApiKey(String grokApiKey) { this.grokApiKey = grokApiKey; }

    public boolean isGrokEnabled() { return grokEnabled; }
    public void setGrokEnabled(boolean grokEnabled) { this.grokEnabled = grokEnabled; }

    public String getOpenAiApiKey() { return openAiApiKey; }
    public void setOpenAiApiKey(String openAiApiKey) { this.openAiApiKey = openAiApiKey; }

    public boolean isOpenAiEnabled() { return openAiEnabled; }
    public void setOpenAiEnabled(boolean openAiEnabled) { this.openAiEnabled = openAiEnabled; }

    public FrontierProvider getPreferredFrontierProvider() { return preferredFrontierProvider; }
    public void setPreferredFrontierProvider(FrontierProvider preferredFrontierProvider) {
        this.preferredFrontierProvider = preferredFrontierProvider != null ? preferredFrontierProvider : FrontierProvider.CLAUDE;
    }

    public boolean isJudgeEnabled() { return judgeEnabled; }
    public void setJudgeEnabled(boolean judgeEnabled) { this.judgeEnabled = judgeEnabled; }

    public String getJudgeModelFilename() { return judgeModelFilename; }
    public void setJudgeModelFilename(String judgeModelFilename) { this.judgeModelFilename = judgeModelFilename; }

    public double getJudgeScoreThreshold() { return judgeScoreThreshold; }
    public void setJudgeScoreThreshold(double judgeScoreThreshold) {
        this.judgeScoreThreshold = judgeScoreThreshold >= 0.0 && judgeScoreThreshold <= 10.0
                ? judgeScoreThreshold : AppConstants.JUDGE_DEFAULT_SCORE_THRESHOLD;
    }

    public Instant getCreatedAt() { return createdAt; }
    public Instant getUpdatedAt() { return updatedAt; }
}
