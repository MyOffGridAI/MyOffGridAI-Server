package com.myoffgridai.privacy.dto;

/**
 * Data transfer object summarizing the count of each data type stored for a user.
 *
 * @author MyOffGridAI
 * @since 0.1.0
 */
public record DataInventory(
        long conversationCount,
        long messageCount,
        long memoryCount,
        long knowledgeDocumentCount,
        long sensorCount,
        long insightCount
) {}
