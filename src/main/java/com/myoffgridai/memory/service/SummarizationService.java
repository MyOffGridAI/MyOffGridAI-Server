package com.myoffgridai.memory.service;

import com.myoffgridai.ai.dto.OllamaChatRequest;
import com.myoffgridai.ai.dto.OllamaChatResponse;
import com.myoffgridai.ai.dto.OllamaMessage;
import com.myoffgridai.ai.model.Conversation;
import com.myoffgridai.ai.model.Message;
import com.myoffgridai.ai.repository.ConversationRepository;
import com.myoffgridai.ai.repository.MessageRepository;
import com.myoffgridai.ai.service.OllamaService;
import com.myoffgridai.config.AppConstants;
import com.myoffgridai.memory.model.Memory;
import com.myoffgridai.system.service.SystemConfigService;
import com.myoffgridai.memory.model.MemoryImportance;
import com.myoffgridai.memory.repository.MemoryRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.PageRequest;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * Service for summarizing conversations into persistent memories.
 *
 * <p>Provides on-demand summarization and a nightly scheduled job that
 * automatically summarizes older conversations with sufficient message count.</p>
 */
@Service
public class SummarizationService {

    private static final Logger log = LoggerFactory.getLogger(SummarizationService.class);

    private final ConversationRepository conversationRepository;
    private final MessageRepository messageRepository;
    private final OllamaService ollamaService;
    private final MemoryService memoryService;
    private final MemoryRepository memoryRepository;
    private final SystemConfigService systemConfigService;

    /**
     * Constructs the summarization service.
     *
     * @param conversationRepository the conversation data access layer
     * @param messageRepository      the message data access layer
     * @param ollamaService          the Ollama integration service
     * @param memoryService          the memory persistence service
     * @param memoryRepository       the memory data access layer
     * @param systemConfigService    the system config service for dynamic AI settings
     */
    public SummarizationService(ConversationRepository conversationRepository,
                                 MessageRepository messageRepository,
                                 OllamaService ollamaService,
                                 MemoryService memoryService,
                                 MemoryRepository memoryRepository,
                                 SystemConfigService systemConfigService) {
        this.conversationRepository = conversationRepository;
        this.messageRepository = messageRepository;
        this.ollamaService = ollamaService;
        this.memoryService = memoryService;
        this.memoryRepository = memoryRepository;
        this.systemConfigService = systemConfigService;
    }

    /**
     * Summarizes a conversation into a CRITICAL importance memory.
     *
     * @param conversationId the conversation to summarize
     * @param userId         the owning user's ID
     * @return the created summary Memory
     */
    public Memory summarizeConversation(UUID conversationId, UUID userId) {
        log.info("Summarizing conversation: {}", conversationId);

        List<Message> messages = messageRepository.findByConversationIdOrderByCreatedAtAsc(conversationId);

        String conversationText = messages.stream()
                .map(m -> m.getRole().name() + ": " + m.getContent())
                .collect(Collectors.joining("\n"));

        String prompt = "Summarize the key points of this conversation in 2-3 sentences, "
                + "focusing on facts about the user and their homestead.\n\n" + conversationText;

        OllamaChatRequest request = new OllamaChatRequest(
                systemConfigService.getAiSettings().modelName(),
                List.of(new OllamaMessage("user", prompt)),
                false,
                Map.of());

        OllamaChatResponse response = ollamaService.chat(request);
        String summary = response.message().content().trim();

        return memoryService.createMemory(
                userId, summary, MemoryImportance.CRITICAL,
                AppConstants.MEMORY_SUMMARIZATION_TAG, conversationId);
    }

    /**
     * Nightly scheduled job that summarizes eligible conversations.
     *
     * <p>Finds conversations older than {@link AppConstants#SUMMARIZATION_AGE_DAYS} days
     * with more than {@link AppConstants#SUMMARIZATION_MIN_MESSAGES} messages that have
     * not yet been summarized, and creates summary memories for each.</p>
     */
    @Scheduled(cron = "0 0 2 * * *")
    public void scheduledNightlySummarization() {
        log.info("Starting nightly conversation summarization");
        int summarized = 0;

        try {
            Instant cutoff = Instant.now().minus(AppConstants.SUMMARIZATION_AGE_DAYS, ChronoUnit.DAYS);

            // Get all conversations (paginated in batches)
            int page = 0;
            int batchSize = 50;
            boolean hasMore = true;

            while (hasMore) {
                var conversations = conversationRepository.findAll(PageRequest.of(page, batchSize));
                hasMore = conversations.hasNext();

                for (Conversation conversation : conversations.getContent()) {
                    if (isEligibleForSummarization(conversation, cutoff)) {
                        try {
                            summarizeConversation(conversation.getId(), conversation.getUser().getId());
                            summarized++;
                        } catch (Exception e) {
                            log.warn("Failed to summarize conversation {}: {}",
                                    conversation.getId(), e.getMessage());
                        }
                    }
                }
                page++;
            }

            log.info("Nightly summarization complete. Summarized {} conversations.", summarized);
        } catch (Exception e) {
            log.error("Nightly summarization job failed: {}", e.getMessage());
        }
    }

    private boolean isEligibleForSummarization(Conversation conversation, Instant cutoff) {
        // Must be old enough
        if (conversation.getCreatedAt().isAfter(cutoff)) {
            return false;
        }

        // Must have enough messages
        if (conversation.getMessageCount() < AppConstants.SUMMARIZATION_MIN_MESSAGES) {
            return false;
        }

        // Must not already be summarized (check for existing summary memory)
        List<Memory> existingSummaries = memoryRepository.findByUserId(conversation.getUser().getId())
                .stream()
                .filter(m -> conversation.getId().equals(m.getSourceConversationId())
                        && m.getTags() != null
                        && m.getTags().contains(AppConstants.MEMORY_SUMMARIZATION_TAG))
                .toList();

        return existingSummaries.isEmpty();
    }
}
