package com.myoffgridai.memory.service;

import com.myoffgridai.common.util.TokenCounter;
import com.myoffgridai.config.AppConstants;
import com.myoffgridai.knowledge.service.SemanticSearchService;
import com.myoffgridai.system.dto.AiSettingsDto;
import com.myoffgridai.system.service.SystemConfigService;
import com.myoffgridai.memory.dto.RagContext;
import com.myoffgridai.memory.model.Memory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

/**
 * Central RAG pipeline coordinator. Assembles context from user memories
 * and knowledge chunks for injection into the system prompt before
 * every Ollama inference call.
 *
 * <p>{@link com.myoffgridai.ai.service.ChatService} calls this service to
 * build context before every chat interaction.</p>
 */
@Service
public class RagService {

    private static final Logger log = LoggerFactory.getLogger(RagService.class);

    private final MemoryService memoryService;
    private final SemanticSearchService semanticSearchService;
    private final SystemConfigService systemConfigService;
    private final EmbeddingService embeddingService;

    /**
     * Constructs the RAG service.
     *
     * @param memoryService         the memory service for retrieving relevant memories
     * @param semanticSearchService the semantic search service for knowledge retrieval
     * @param systemConfigService   the system config service for dynamic AI settings
     * @param embeddingService      the embedding service for generating query embeddings
     */
    public RagService(MemoryService memoryService,
                       SemanticSearchService semanticSearchService,
                       SystemConfigService systemConfigService,
                       EmbeddingService embeddingService) {
        this.memoryService = memoryService;
        this.semanticSearchService = semanticSearchService;
        this.systemConfigService = systemConfigService;
        this.embeddingService = embeddingService;
    }

    /**
     * Builds the RAG context for a user's query by retrieving relevant
     * memories and knowledge chunks.
     *
     * @param userId    the user's ID
     * @param queryText the user's message to find context for
     * @return the assembled RAG context
     */
    public RagContext buildRagContext(UUID userId, String queryText) {
        log.debug("Building RAG context for user: {}", userId);

        // Read dynamic AI settings
        AiSettingsDto aiSettings = systemConfigService.getAiSettings();
        int memoryTopK = aiSettings.memoryTopK();
        int ragTopK = AppConstants.RAG_TOP_K;

        // Embed once — shared by both memory and knowledge searches
        float[] queryEmbedding;
        try {
            long embedStart = System.currentTimeMillis();
            queryEmbedding = embeddingService.embed(queryText);
            log.info("[TIMING] Embedding call: {}ms", System.currentTimeMillis() - embedStart);
        } catch (Exception e) {
            log.warn("Failed to embed query for RAG context: {}. Returning empty context.", e.getMessage());
            return new RagContext(List.of(), List.of(), false, 0);
        }

        // Launch both searches in parallel — total time = max(memory, knowledge) instead of sum
        CompletableFuture<List<String>> memoryFuture = CompletableFuture.supplyAsync(() -> {
            try {
                List<Memory> memories = memoryService.findRelevantMemories(
                        userId, queryText, memoryTopK, queryEmbedding);
                List<String> snippets = new ArrayList<>();
                for (Memory memory : memories) {
                    snippets.add(memory.getContent());
                }
                return snippets;
            } catch (Exception e) {
                log.warn("Failed to retrieve memories for RAG context: {}", e.getMessage());
                return List.of();
            }
        });

        CompletableFuture<List<String>> knowledgeFuture = CompletableFuture.supplyAsync(() -> {
            try {
                return semanticSearchService.searchForRagContext(
                        userId, queryText, ragTopK, queryEmbedding);
            } catch (Exception e) {
                log.debug("No knowledge chunks available for RAG context: {}", e.getMessage());
                return List.of();
            }
        });

        // Wait for both searches to complete
        long searchStart = System.currentTimeMillis();
        CompletableFuture.allOf(memoryFuture, knowledgeFuture).join();
        log.info("[TIMING] Parallel searches (memory + knowledge): {}ms", System.currentTimeMillis() - searchStart);

        List<String> memorySnippets = memoryFuture.join();
        List<String> knowledgeSnippets = knowledgeFuture.join();

        boolean hasContext = !memorySnippets.isEmpty() || !knowledgeSnippets.isEmpty();
        int tokenEstimate = estimateContextTokens(memorySnippets, knowledgeSnippets);

        RagContext context = new RagContext(memorySnippets, knowledgeSnippets, hasContext, tokenEstimate);
        log.debug("RAG context built: {} memories, {} knowledge chunks, {} estimated tokens",
                memorySnippets.size(), knowledgeSnippets.size(), tokenEstimate);
        return context;
    }

    /**
     * Formats a {@link RagContext} into a string block for injection into the system prompt.
     *
     * @param context the RAG context to format
     * @return the formatted context string, or empty string if no context
     */
    public String formatContextBlock(RagContext context) {
        if (context == null || !context.hasContext()) {
            return "";
        }

        StringBuilder sb = new StringBuilder();

        if (!context.memorySnippets().isEmpty()) {
            sb.append("[RELEVANT MEMORIES]\n");
            for (String snippet : context.memorySnippets()) {
                sb.append("- ").append(snippet).append("\n");
            }
            sb.append("[END MEMORIES]\n");
        }

        if (!context.knowledgeSnippets().isEmpty()) {
            sb.append("[RELEVANT KNOWLEDGE]\n");
            for (String snippet : context.knowledgeSnippets()) {
                sb.append("- ").append(snippet).append("\n");
            }
            sb.append("[END KNOWLEDGE]\n");
        }

        return sb.toString();
    }

    private int estimateContextTokens(List<String> memorySnippets, List<String> knowledgeSnippets) {
        int total = 0;
        for (String s : memorySnippets) {
            total += TokenCounter.estimateTokens(s);
        }
        for (String s : knowledgeSnippets) {
            total += TokenCounter.estimateTokens(s);
        }
        return total;
    }
}
