package com.myoffgridai.ai.service;

import com.myoffgridai.ai.dto.OllamaMessage;
import com.myoffgridai.ai.model.Message;
import com.myoffgridai.ai.repository.MessageRepository;
import com.myoffgridai.common.util.TokenCounter;
import com.myoffgridai.system.dto.AiSettingsDto;
import com.myoffgridai.system.service.SystemConfigService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.UUID;

/**
 * Manages the context window for Ollama chat requests.
 *
 * <p>Assembles the message list from conversation history, prepends the
 * system prompt, appends the new user message, and truncates to fit
 * within the configured token limit.</p>
 */
@Service
public class ContextWindowService {

    private static final Logger log = LoggerFactory.getLogger(ContextWindowService.class);

    private final MessageRepository messageRepository;
    private final SystemConfigService systemConfigService;

    /**
     * Constructs the context window service.
     *
     * @param messageRepository   the message data access layer
     * @param systemConfigService the system config service for dynamic AI settings
     */
    public ContextWindowService(MessageRepository messageRepository,
                                SystemConfigService systemConfigService) {
        this.messageRepository = messageRepository;
        this.systemConfigService = systemConfigService;
    }

    /**
     * Prepares an ordered message list for an Ollama chat request.
     *
     * <p>Fetches recent messages from the conversation (up to the configured
     * limit), prepends the system prompt, appends the new user message, and
     * truncates to fit within the maximum context token limit.</p>
     *
     * @param conversationId the conversation to load history from
     * @param systemPrompt   the system prompt text
     * @param newUserMessage the new user message to append
     * @return the final ordered message list ready for Ollama
     */
    public List<OllamaMessage> prepareMessages(UUID conversationId, String systemPrompt, String newUserMessage) {
        log.debug("Preparing context window for conversation: {}", conversationId);

        AiSettingsDto aiSettings = systemConfigService.getAiSettings();

        // Fetch recent messages (most recent N, ordered newest-first)
        List<Message> recentMessages = messageRepository.findTopNByConversationIdOrderByCreatedAtDesc(
                conversationId,
                PageRequest.of(0, aiSettings.contextMessageLimit())
        );

        // Reverse to chronological order
        List<Message> chronological = new ArrayList<>(recentMessages);
        Collections.reverse(chronological);

        // Build the message list
        List<OllamaMessage> messages = new ArrayList<>();
        messages.add(new OllamaMessage("system", systemPrompt));

        for (Message msg : chronological) {
            messages.add(new OllamaMessage(msg.getRole().name().toLowerCase(), msg.getContent()));
        }

        messages.add(new OllamaMessage("user", newUserMessage));

        // Truncate to token limit using the configured context size
        List<OllamaMessage> truncated = TokenCounter.truncateToTokenLimit(
                messages, aiSettings.contextSize());

        log.debug("Context window: {} messages, estimated {} tokens (limit: {} tokens, {} messages)",
                truncated.size(),
                truncated.stream().mapToInt(m -> TokenCounter.estimateTokens(m.content())).sum(),
                aiSettings.contextSize(), aiSettings.contextMessageLimit());

        return truncated;
    }
}
