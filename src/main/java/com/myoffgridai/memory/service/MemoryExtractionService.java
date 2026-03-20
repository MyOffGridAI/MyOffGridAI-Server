package com.myoffgridai.memory.service;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.myoffgridai.ai.dto.OllamaChatRequest;
import com.myoffgridai.ai.dto.OllamaChatResponse;
import com.myoffgridai.ai.dto.OllamaMessage;
import com.myoffgridai.ai.service.OllamaService;
import com.myoffgridai.config.AppConstants;
import com.myoffgridai.memory.model.MemoryImportance;
import com.myoffgridai.system.service.SystemConfigService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Extracts key facts from conversation exchanges and stores them as persistent memories.
 *
 * <p>Runs asynchronously after each assistant response to avoid blocking the chat flow.
 * Uses Ollama to identify memorable facts (preferences, personal details, homestead
 * specifics) and persists them via {@link MemoryService}.</p>
 */
@Service
public class MemoryExtractionService {

    private static final Logger log = LoggerFactory.getLogger(MemoryExtractionService.class);

    private final OllamaService ollamaService;
    private final MemoryService memoryService;
    private final ObjectMapper objectMapper;
    private final SystemConfigService systemConfigService;

    /**
     * Constructs the memory extraction service.
     *
     * @param ollamaService       the Ollama integration service
     * @param memoryService       the memory persistence service
     * @param objectMapper        the JSON object mapper
     * @param systemConfigService the system config service for dynamic AI settings
     */
    public MemoryExtractionService(OllamaService ollamaService,
                                    MemoryService memoryService,
                                    ObjectMapper objectMapper,
                                    SystemConfigService systemConfigService) {
        this.ollamaService = ollamaService;
        this.memoryService = memoryService;
        this.objectMapper = objectMapper;
        this.systemConfigService = systemConfigService;
    }

    /**
     * Stores frontier-enhanced knowledge as a persistent memory for future RAG retrieval.
     *
     * <p>When the Judge triggers a frontier API call and gets an enhanced response,
     * this method stores the user query and enhanced answer as a HIGH importance
     * memory. This effectively expands the local offline model's knowledge by
     * making frontier responses available through the RAG pipeline for similar
     * future queries.</p>
     *
     * <p>Runs asynchronously — does not block the chat response.</p>
     *
     * @param userId           the user's ID
     * @param conversationId   the source conversation ID
     * @param userQuery        the original user query
     * @param enhancedResponse the frontier-enhanced response content
     */
    @Async
    public void storeFrontierKnowledge(UUID userId, UUID conversationId,
                                        String userQuery, String enhancedResponse) {
        log.info("Storing frontier knowledge for user: {} from conversation: {}", userId, conversationId);
        try {
            String content = "Q: " + userQuery.trim() + "\nA: " + enhancedResponse.trim();
            memoryService.createMemory(userId, content, MemoryImportance.HIGH,
                    "frontier,enhanced", conversationId);
            log.info("Stored frontier knowledge memory for conversation: {}", conversationId);
        } catch (Exception e) {
            log.warn("Failed to store frontier knowledge for conversation {}: {}",
                    conversationId, e.getMessage());
        }
    }

    /**
     * Extracts memorable facts from a conversation exchange and stores them.
     *
     * <p>Runs asynchronously — does not block the chat response. If extraction
     * or parsing fails, logs a warning and returns without throwing.</p>
     *
     * @param userId            the user's ID
     * @param conversationId    the conversation that produced this exchange
     * @param userMessage       the user's message
     * @param assistantResponse the assistant's response
     */
    @Async
    public void extractAndStore(UUID userId, UUID conversationId,
                                 String userMessage, String assistantResponse) {
        log.debug("Extracting memories from conversation: {}", conversationId);

        try {
            String prompt = String.format("""
                    Extract 0-%d important facts about the user from this conversation exchange.
                    Only extract facts that would be useful to remember long-term (preferences, possessions, \
                    plans, personal details, homestead specifics). If there are no memorable facts, return empty.

                    Respond ONLY with a JSON array of objects, each with:
                    - "content": the fact as a single sentence
                    - "importance": one of LOW, MEDIUM, HIGH, CRITICAL
                    - "tags": comma-separated relevant tags

                    User said: %s
                    Assistant responded: %s

                    JSON array only, no other text:""",
                    AppConstants.MEMORY_EXTRACTION_MAX_FACTS, userMessage, assistantResponse);

            var aiSettings = systemConfigService.getAiSettings();
            OllamaChatRequest request = new OllamaChatRequest(
                    aiSettings.modelName(),
                    List.of(new OllamaMessage("user", prompt)),
                    false,
                    Map.of("num_ctx", aiSettings.contextSize(),
                            "num_predict", 512));

            OllamaChatResponse response = ollamaService.chat(request);
            String responseText = response.message().content().trim();

            parseAndStoreMemories(userId, conversationId, responseText);
        } catch (Exception e) {
            log.warn("Memory extraction failed for conversation {}: {}", conversationId, e.getMessage());
        }
    }

    private void parseAndStoreMemories(UUID userId, UUID conversationId, String jsonResponse) {
        try {
            // Strip markdown code fences if present
            String cleaned = jsonResponse;
            if (cleaned.startsWith("```")) {
                cleaned = cleaned.replaceAll("^```\\w*\\n?", "").replaceAll("\\n?```$", "");
            }
            cleaned = cleaned.trim();

            if (cleaned.isEmpty() || cleaned.equals("[]")) {
                log.debug("No memories to extract from conversation: {}", conversationId);
                return;
            }

            List<Map<String, String>> facts = objectMapper.readValue(
                    cleaned, new TypeReference<>() {});

            int stored = 0;
            for (Map<String, String> fact : facts) {
                String content = fact.get("content");
                String importanceStr = fact.getOrDefault("importance", "MEDIUM");
                String tags = fact.getOrDefault("tags", "");

                if (content == null || content.isBlank()) {
                    continue;
                }

                MemoryImportance importance;
                try {
                    importance = MemoryImportance.valueOf(importanceStr.toUpperCase());
                } catch (IllegalArgumentException e) {
                    importance = MemoryImportance.MEDIUM;
                }

                memoryService.createMemory(userId, content, importance, tags, conversationId);
                stored++;
            }

            log.debug("Extracted and stored {} memories from conversation: {}", stored, conversationId);
        } catch (Exception e) {
            log.warn("Failed to parse memory extraction response: {}", e.getMessage());
        }
    }
}
