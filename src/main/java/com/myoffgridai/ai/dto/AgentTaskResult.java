package com.myoffgridai.ai.dto;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

/**
 * Data transfer object representing the result of an agent-driven multi-step task execution.
 *
 * @author MyOffGridAI
 * @since 0.1.0
 */
public record AgentTaskResult(
        UUID conversationId,
        String finalResponse,
        List<String> detectedToolCalls,
        int stepCount,
        Instant completedAt
) {
}
