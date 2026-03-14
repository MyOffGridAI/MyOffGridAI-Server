package com.myoffgridai.memory.service;

import com.myoffgridai.common.util.TokenCounter;
import com.myoffgridai.config.AppConstants;
import com.myoffgridai.memory.dto.RagContext;
import com.myoffgridai.memory.model.Memory;
import com.myoffgridai.memory.model.VectorDocument;
import com.myoffgridai.memory.model.VectorSourceType;
import com.myoffgridai.memory.repository.VectorDocumentRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

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
    private final VectorDocumentRepository vectorDocumentRepository;
    private final EmbeddingService embeddingService;

    /**
     * Constructs the RAG service.
     *
     * @param memoryService            the memory service for retrieving relevant memories
     * @param vectorDocumentRepository the vector document repository for knowledge chunks
     * @param embeddingService         the embedding service for query embedding
     */
    public RagService(MemoryService memoryService,
                       VectorDocumentRepository vectorDocumentRepository,
                       EmbeddingService embeddingService) {
        this.memoryService = memoryService;
        this.vectorDocumentRepository = vectorDocumentRepository;
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

        // Retrieve relevant memories
        List<String> memorySnippets = new ArrayList<>();
        try {
            List<Memory> memories = memoryService.findRelevantMemories(
                    userId, queryText, AppConstants.MEMORY_TOP_K);
            for (Memory memory : memories) {
                memorySnippets.add(memory.getContent());
            }
        } catch (Exception e) {
            log.warn("Failed to retrieve memories for RAG context: {}", e.getMessage());
        }

        // Retrieve relevant knowledge chunks (Phase 4 will populate these)
        List<String> knowledgeSnippets = new ArrayList<>();
        try {
            String formattedEmbedding = embeddingService.embedAndFormat(queryText);
            List<VectorDocument> knowledgeDocs = vectorDocumentRepository.findMostSimilar(
                    userId, VectorSourceType.KNOWLEDGE_CHUNK.name(),
                    formattedEmbedding, AppConstants.RAG_TOP_K);
            for (VectorDocument doc : knowledgeDocs) {
                knowledgeSnippets.add(doc.getContent());
            }
        } catch (Exception e) {
            log.debug("No knowledge chunks available for RAG context: {}", e.getMessage());
        }

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
