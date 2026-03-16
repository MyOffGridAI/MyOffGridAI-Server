package com.myoffgridai.proactive.dto;

import com.myoffgridai.proactive.model.Insight;
import com.myoffgridai.proactive.model.InsightCategory;

import java.time.Instant;
import java.util.UUID;

/**
 * Data transfer object representing an AI-generated proactive insight.
 *
 * @author MyOffGridAI
 * @since 0.1.0
 */
public record InsightDto(
        UUID id,
        String content,
        InsightCategory category,
        boolean isRead,
        boolean isDismissed,
        Instant generatedAt,
        Instant readAt
) {

    public static InsightDto from(Insight insight) {
        return new InsightDto(
                insight.getId(),
                insight.getContent(),
                insight.getCategory(),
                insight.getIsRead(),
                insight.getIsDismissed(),
                insight.getGeneratedAt(),
                insight.getReadAt()
        );
    }
}
