package com.myoffgridai.knowledge.model;

/**
 * Processing status of a knowledge document through the ingestion pipeline.
 *
 * @author MyOffGridAI
 * @since 0.1.0
 */
public enum DocumentStatus {
    PENDING,
    PROCESSING,
    READY,
    FAILED
}
