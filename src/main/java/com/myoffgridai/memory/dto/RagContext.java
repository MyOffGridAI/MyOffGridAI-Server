package com.myoffgridai.memory.dto;

import java.util.List;

/**
 * Holds the assembled RAG context for injection into the system prompt.
 *
 * @param memorySnippets    relevant memory content strings ordered by similarity
 * @param knowledgeSnippets relevant knowledge chunk content strings (empty in Phase 3)
 * @param hasContext        true if either list is non-empty
 * @param tokenEstimate     estimated token count of the assembled context
 */
public record RagContext(
        List<String> memorySnippets,
        List<String> knowledgeSnippets,
        boolean hasContext,
        int tokenEstimate
) {}
