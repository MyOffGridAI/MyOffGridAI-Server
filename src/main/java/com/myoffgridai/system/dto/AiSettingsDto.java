package com.myoffgridai.system.dto;

/**
 * Data transfer object for AI and memory configuration settings.
 *
 * @param temperature          the LLM temperature (0.0–2.0)
 * @param similarityThreshold  the minimum cosine similarity for memory/RAG results (0.0–1.0)
 * @param memoryTopK           the number of top-K memory results to retrieve (1–20)
 * @param ragMaxContextTokens  the maximum tokens for RAG context injection (512–8192)
 */
public record AiSettingsDto(
        Double temperature,
        Double similarityThreshold,
        Integer memoryTopK,
        Integer ragMaxContextTokens
) {}
