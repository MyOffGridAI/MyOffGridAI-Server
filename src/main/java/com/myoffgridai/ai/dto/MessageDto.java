package com.myoffgridai.ai.dto;

import com.myoffgridai.ai.SourceTag;
import com.myoffgridai.ai.model.MessageRole;

import java.time.Instant;
import java.util.UUID;

/**
 * Data transfer object representing a single message within a conversation.
 *
 * @author MyOffGridAI
 * @since 0.1.0
 */
public record MessageDto(
        UUID id,
        MessageRole role,
        String content,
        Integer tokenCount,
        boolean hasRagContext,
        String thinkingContent,
        Double tokensPerSecond,
        Double inferenceTimeSeconds,
        String stopReason,
        Integer thinkingTokenCount,
        SourceTag sourceTag,
        Double judgeScore,
        String judgeReason,
        Instant createdAt
) {
}
