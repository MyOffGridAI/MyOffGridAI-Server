package com.myoffgridai.privacy.dto;

public record DataInventory(
        long conversationCount,
        long messageCount,
        long memoryCount,
        long knowledgeDocumentCount,
        long sensorCount,
        long insightCount
) {}
