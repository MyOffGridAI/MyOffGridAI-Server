package com.myoffgridai.ai.dto;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

public record AgentTaskResult(
        UUID conversationId,
        String finalResponse,
        List<String> detectedToolCalls,
        int stepCount,
        Instant completedAt
) {
}
