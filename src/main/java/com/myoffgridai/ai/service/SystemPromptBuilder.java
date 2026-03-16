package com.myoffgridai.ai.service;

import com.myoffgridai.auth.model.User;
import com.myoffgridai.common.util.TokenCounter;
import com.myoffgridai.config.AppConstants;
import com.myoffgridai.memory.dto.RagContext;
import com.myoffgridai.memory.service.RagService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

/**
 * Builds the system prompt injected before every conversation with Ollama.
 *
 * <p>In Phase 3, the prompt includes user context and RAG-injected memory
 * and knowledge snippets. The {@code <!-- MEMORY_CONTEXT -->} and
 * {@code <!-- RAG_CONTEXT -->} comment markers are replaced with actual context
 * when available.</p>
 */
@Service
public class SystemPromptBuilder {

    private static final Logger log = LoggerFactory.getLogger(SystemPromptBuilder.class);

    private final RagService ragService;

    /**
     * Constructs the system prompt builder.
     *
     * @param ragService the RAG service for formatting context blocks
     */
    public SystemPromptBuilder(RagService ragService) {
        this.ragService = ragService;
    }

    /**
     * Assembles a system prompt without RAG context (backward compatible).
     *
     * @param user         the authenticated user
     * @param instanceName the name of this MyOffGridAI instance
     * @return the assembled system prompt
     */
    public String build(User user, String instanceName) {
        return build(user, instanceName, null);
    }

    /**
     * Assembles a system prompt with optional RAG context injection.
     *
     * <p>When {@code ragContext} has context, the comment markers are replaced
     * with formatted memory and knowledge snippets. The injected context is kept
     * under {@link AppConstants#RAG_MAX_CONTEXT_TOKENS} — knowledge snippets are
     * truncated first, then memories.</p>
     *
     * @param user         the authenticated user
     * @param instanceName the name of this MyOffGridAI instance
     * @param ragContext   the RAG context to inject (nullable)
     * @return the assembled system prompt, kept under 500 tokens for base prompt
     */
    public String build(User user, String instanceName, RagContext ragContext) {
        log.debug("Building system prompt for user: {}, instance: {}, hasRag: {}",
                user.getUsername(), instanceName, ragContext != null && ragContext.hasContext());

        String now = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm")
                .withZone(ZoneOffset.UTC)
                .format(Instant.now());

        String basePrompt = String.format("""
                You are %s, the MyOffGrid AI assistant for %s.
                You run entirely offline on a private device. All data stays on this device and is never shared.

                Current date/time (UTC): %s
                User: %s (role: %s)

                %s

                Be helpful, concise, and accurate. If you don't know something, say so.""",
                instanceName,
                user.getDisplayName(),
                now,
                user.getDisplayName(),
                user.getRole().name(),
                buildContextSection(ragContext)
        );

        return basePrompt;
    }

    private String buildContextSection(RagContext ragContext) {
        if (ragContext == null || !ragContext.hasContext()) {
            return "";
        }

        // Truncate context to fit within RAG_MAX_CONTEXT_TOKENS
        RagContext truncated = truncateContext(ragContext);
        return ragService.formatContextBlock(truncated);
    }

    private RagContext truncateContext(RagContext context) {
        int totalTokens = context.tokenEstimate();
        if (totalTokens <= AppConstants.RAG_MAX_CONTEXT_TOKENS) {
            return context;
        }

        // Truncate knowledge first, then memories
        List<String> knowledge = new ArrayList<>(context.knowledgeSnippets());
        List<String> memories = new ArrayList<>(context.memorySnippets());

        int currentTokens = totalTokens;

        // Remove knowledge snippets from end until under limit
        while (currentTokens > AppConstants.RAG_MAX_CONTEXT_TOKENS && !knowledge.isEmpty()) {
            String removed = knowledge.remove(knowledge.size() - 1);
            currentTokens -= TokenCounter.estimateTokens(removed);
        }

        // Remove memory snippets from end until under limit
        while (currentTokens > AppConstants.RAG_MAX_CONTEXT_TOKENS && !memories.isEmpty()) {
            String removed = memories.remove(memories.size() - 1);
            currentTokens -= TokenCounter.estimateTokens(removed);
        }

        boolean hasContext = !memories.isEmpty() || !knowledge.isEmpty();
        return new RagContext(memories, knowledge, hasContext, currentTokens);
    }
}
