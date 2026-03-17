package com.myoffgridai.ai.model;

import com.myoffgridai.ai.SourceTag;
import jakarta.persistence.*;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.Instant;
import java.util.UUID;

/**
 * Persistent entity representing a single message within an AI conversation.
 *
 * @author MyOffGridAI
 * @since 0.1.0
 */
@Entity
@Table(name = "messages")
@EntityListeners(AuditingEntityListener.class)
public class Message {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "conversation_id", nullable = false)
    private Conversation conversation;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private MessageRole role;

    @Column(nullable = false, columnDefinition = "TEXT")
    private String content;

    @Column(name = "token_count")
    private Integer tokenCount;

    @Column(name = "has_rag_context", nullable = false)
    private boolean hasRagContext = false;

    @Column(name = "thinking_content", columnDefinition = "TEXT")
    private String thinkingContent;

    @Column(name = "tokens_per_second")
    private Double tokensPerSecond;

    @Column(name = "inference_time_seconds")
    private Double inferenceTimeSeconds;

    @Column(name = "stop_reason")
    private String stopReason;

    @Column(name = "thinking_token_count")
    private Integer thinkingTokenCount;

    /** Indicates whether the response is local-only or was enhanced by a cloud frontier model. */
    @Enumerated(EnumType.STRING)
    @Column(name = "source_tag", length = 20, nullable = false)
    private SourceTag sourceTag = SourceTag.LOCAL;

    /** Quality score assigned by the AI judge model (1–10), or null if judge was not used. */
    @Column(name = "judge_score")
    private Double judgeScore;

    /** Brief explanation from the AI judge for the assigned score. */
    @Column(name = "judge_reason", columnDefinition = "TEXT")
    private String judgeReason;

    @CreatedDate
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    public Message() {
    }

    @PrePersist
    protected void onCreate() {
        if (createdAt == null) {
            createdAt = Instant.now();
        }
    }

    // ── Getters and Setters ─────────────────────────────────────────────────

    public UUID getId() {
        return id;
    }

    public void setId(UUID id) {
        this.id = id;
    }

    public Conversation getConversation() {
        return conversation;
    }

    public void setConversation(Conversation conversation) {
        this.conversation = conversation;
    }

    public MessageRole getRole() {
        return role;
    }

    public void setRole(MessageRole role) {
        this.role = role;
    }

    public String getContent() {
        return content;
    }

    public void setContent(String content) {
        this.content = content;
    }

    public Integer getTokenCount() {
        return tokenCount;
    }

    public void setTokenCount(Integer tokenCount) {
        this.tokenCount = tokenCount;
    }

    public boolean getHasRagContext() {
        return hasRagContext;
    }

    public void setHasRagContext(boolean hasRagContext) {
        this.hasRagContext = hasRagContext;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(Instant createdAt) {
        this.createdAt = createdAt;
    }

    public String getThinkingContent() {
        return thinkingContent;
    }

    public void setThinkingContent(String thinkingContent) {
        this.thinkingContent = thinkingContent;
    }

    public Double getTokensPerSecond() {
        return tokensPerSecond;
    }

    public void setTokensPerSecond(Double tokensPerSecond) {
        this.tokensPerSecond = tokensPerSecond;
    }

    public Double getInferenceTimeSeconds() {
        return inferenceTimeSeconds;
    }

    public void setInferenceTimeSeconds(Double inferenceTimeSeconds) {
        this.inferenceTimeSeconds = inferenceTimeSeconds;
    }

    public String getStopReason() {
        return stopReason;
    }

    public void setStopReason(String stopReason) {
        this.stopReason = stopReason;
    }

    public Integer getThinkingTokenCount() {
        return thinkingTokenCount;
    }

    public void setThinkingTokenCount(Integer thinkingTokenCount) {
        this.thinkingTokenCount = thinkingTokenCount;
    }

    public SourceTag getSourceTag() {
        return sourceTag;
    }

    public void setSourceTag(SourceTag sourceTag) {
        this.sourceTag = sourceTag != null ? sourceTag : SourceTag.LOCAL;
    }

    public Double getJudgeScore() {
        return judgeScore;
    }

    public void setJudgeScore(Double judgeScore) {
        this.judgeScore = judgeScore;
    }

    public String getJudgeReason() {
        return judgeReason;
    }

    public void setJudgeReason(String judgeReason) {
        this.judgeReason = judgeReason;
    }
}
