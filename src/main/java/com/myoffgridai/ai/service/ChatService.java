package com.myoffgridai.ai.service;

import com.myoffgridai.ai.dto.ChunkType;
import com.myoffgridai.ai.dto.InferenceChunk;
import com.myoffgridai.ai.dto.OllamaChatRequest;
import com.myoffgridai.ai.dto.OllamaChatResponse;
import com.myoffgridai.ai.dto.OllamaMessage;
import com.myoffgridai.ai.model.Conversation;
import com.myoffgridai.ai.model.Message;
import com.myoffgridai.ai.model.MessageRole;
import com.myoffgridai.ai.repository.ConversationRepository;
import com.myoffgridai.ai.repository.MessageRepository;
import com.myoffgridai.auth.model.User;
import com.myoffgridai.auth.repository.UserRepository;
import com.myoffgridai.common.exception.EntityNotFoundException;
import com.myoffgridai.common.util.TokenCounter;
import com.myoffgridai.config.AppConstants;
import com.myoffgridai.memory.dto.RagContext;
import com.myoffgridai.memory.service.MemoryExtractionService;
import com.myoffgridai.memory.service.RagService;
import com.myoffgridai.system.dto.AiSettingsDto;
import com.myoffgridai.system.service.SystemConfigService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import reactor.core.publisher.Flux;

import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Core chat service managing conversations, messages, and Ollama interactions.
 *
 * <p>Handles conversation lifecycle (create, list, archive, delete) and message
 * exchange (synchronous and streaming) with the Ollama LLM via {@link OllamaService}.
 * Integrates RAG context from {@link RagService} and triggers asynchronous memory
 * extraction via {@link MemoryExtractionService} after each exchange.</p>
 */
@Service
public class ChatService {

    private static final Logger log = LoggerFactory.getLogger(ChatService.class);

    private final ConversationRepository conversationRepository;
    private final MessageRepository messageRepository;
    private final UserRepository userRepository;
    private final OllamaService ollamaService;
    private final InferenceService inferenceService;
    private final SystemPromptBuilder systemPromptBuilder;
    private final ContextWindowService contextWindowService;
    private final RagService ragService;
    private final MemoryExtractionService memoryExtractionService;
    private final SystemConfigService systemConfigService;

    /**
     * Constructs the chat service with required dependencies.
     *
     * @param conversationRepository  the conversation data access layer
     * @param messageRepository       the message data access layer
     * @param userRepository          the user data access layer
     * @param ollamaService           the Ollama integration service
     * @param inferenceService        the inference abstraction service
     * @param systemPromptBuilder     the system prompt builder
     * @param contextWindowService    the context window manager
     * @param ragService              the RAG pipeline service
     * @param memoryExtractionService the memory extraction service
     * @param systemConfigService     the system config service for dynamic AI settings
     */
    public ChatService(ConversationRepository conversationRepository,
                       MessageRepository messageRepository,
                       UserRepository userRepository,
                       OllamaService ollamaService,
                       InferenceService inferenceService,
                       SystemPromptBuilder systemPromptBuilder,
                       ContextWindowService contextWindowService,
                       RagService ragService,
                       MemoryExtractionService memoryExtractionService,
                       SystemConfigService systemConfigService) {
        this.conversationRepository = conversationRepository;
        this.messageRepository = messageRepository;
        this.userRepository = userRepository;
        this.ollamaService = ollamaService;
        this.inferenceService = inferenceService;
        this.systemPromptBuilder = systemPromptBuilder;
        this.contextWindowService = contextWindowService;
        this.ragService = ragService;
        this.memoryExtractionService = memoryExtractionService;
        this.systemConfigService = systemConfigService;
    }

    /**
     * Creates a new conversation for the given user.
     *
     * @param userId       the user's ID
     * @param initialTitle an optional initial title (null until auto-generated)
     * @return the created conversation
     */
    @Transactional
    public Conversation createConversation(UUID userId, String initialTitle) {
        log.info("Creating conversation for user: {}", userId);
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new EntityNotFoundException("User not found: " + userId));

        Conversation conversation = new Conversation();
        conversation.setUser(user);
        conversation.setTitle(initialTitle);

        Conversation saved = conversationRepository.save(conversation);
        log.info("Created conversation: {}", saved.getId());
        return saved;
    }

    /**
     * Returns paginated conversations for the user, newest first.
     *
     * @param userId          the user's ID
     * @param includeArchived whether to include archived conversations
     * @param pageable        pagination parameters
     * @return a page of conversations
     */
    public Page<Conversation> getConversations(UUID userId, boolean includeArchived, Pageable pageable) {
        log.debug("Listing conversations for user: {}, includeArchived: {}", userId, includeArchived);
        if (includeArchived) {
            return conversationRepository.findByUserIdOrderByUpdatedAtDesc(userId, pageable);
        }
        return conversationRepository.findByUserIdAndIsArchivedOrderByUpdatedAtDesc(userId, false, pageable);
    }

    /**
     * Fetches a conversation by ID, enforcing user ownership.
     *
     * @param conversationId the conversation ID
     * @param userId         the user's ID
     * @return the conversation
     * @throws EntityNotFoundException if not found or not owned by user
     */
    public Conversation getConversation(UUID conversationId, UUID userId) {
        log.debug("Getting conversation: {} for user: {}", conversationId, userId);
        return conversationRepository.findByIdAndUserId(conversationId, userId)
                .orElseThrow(() -> new EntityNotFoundException("Conversation not found: " + conversationId));
    }

    /**
     * Archives a conversation.
     *
     * @param conversationId the conversation ID
     * @param userId         the user's ID
     */
    @Transactional
    public void archiveConversation(UUID conversationId, UUID userId) {
        log.info("Archiving conversation: {} for user: {}", conversationId, userId);
        Conversation conversation = getConversation(conversationId, userId);
        conversation.setIsArchived(true);
        conversationRepository.save(conversation);
    }

    /**
     * Deletes a conversation and all its messages.
     *
     * @param conversationId the conversation ID
     * @param userId         the user's ID
     */
    @Transactional
    public void deleteConversation(UUID conversationId, UUID userId) {
        log.info("Deleting conversation: {} for user: {}", conversationId, userId);
        Conversation conversation = getConversation(conversationId, userId);
        messageRepository.deleteByConversationId(conversationId);
        conversationRepository.delete(conversation);
    }

    /**
     * Sends a message synchronously and returns the assistant's response.
     *
     * <p>Persists the user message, builds RAG context, builds the context window,
     * calls Ollama synchronously, persists the assistant response, triggers async
     * title generation on the first exchange, and triggers async memory extraction.</p>
     *
     * @param conversationId the conversation ID
     * @param userId         the user's ID
     * @param userContent    the user's message content
     * @return the persisted assistant message
     */
    @Transactional
    public Message sendMessage(UUID conversationId, UUID userId, String userContent) {
        log.info("Sending message in conversation: {} for user: {}", conversationId, userId);

        Conversation conversation = getConversation(conversationId, userId);
        User user = conversation.getUser();
        boolean isFirstExchange = conversation.getMessageCount() == 0;

        // Persist user message
        Message userMessage = new Message();
        userMessage.setConversation(conversation);
        userMessage.setRole(MessageRole.USER);
        userMessage.setContent(userContent);
        userMessage.setTokenCount(TokenCounter.estimateTokens(userContent));
        messageRepository.save(userMessage);

        // Build RAG context
        RagContext ragContext = buildRagContextSafely(userId, userContent);

        // Build context window
        String systemPrompt = systemPromptBuilder.build(user, "MyOffGridAI", ragContext);
        List<OllamaMessage> messages = contextWindowService.prepareMessages(
                conversationId, systemPrompt, userContent);

        // Call Ollama synchronously with dynamic model, temperature, and context size
        AiSettingsDto aiSettings = systemConfigService.getAiSettings();
        OllamaChatRequest request = new OllamaChatRequest(
                aiSettings.modelName(), messages, false,
                Map.of("temperature", aiSettings.temperature(), "num_ctx", aiSettings.contextSize()));
        OllamaChatResponse response = ollamaService.chat(request);

        // Persist assistant message
        Message assistantMessage = new Message();
        assistantMessage.setConversation(conversation);
        assistantMessage.setRole(MessageRole.ASSISTANT);
        assistantMessage.setContent(response.message().content());
        assistantMessage.setTokenCount(response.evalCount());
        assistantMessage.setHasRagContext(ragContext != null && ragContext.hasContext());
        messageRepository.save(assistantMessage);

        // Update conversation message count
        conversation.setMessageCount(conversation.getMessageCount() + 2);
        conversationRepository.save(conversation);

        // Trigger async title generation on first exchange
        if (isFirstExchange) {
            generateTitle(conversationId, userContent);
        }

        // Trigger async memory extraction
        memoryExtractionService.extractAndStore(
                userId, conversationId, userContent, response.message().content());

        log.info("Message exchange complete in conversation: {}", conversationId);
        return assistantMessage;
    }

    /**
     * Sends a message and returns a streaming response as a Flux of typed JSON events.
     *
     * <p>Uses {@link InferenceService#streamChatWithThinking(List, UUID)} to emit
     * thinking and content chunks separately. Each chunk is serialized as a JSON
     * event with a {@code type} field: "thinking", "content", "done", or "error".</p>
     *
     * @param conversationId the conversation ID
     * @param userId         the user's ID
     * @param userContent    the user's message content
     * @return a Flux emitting JSON event strings
     */
    public Flux<String> streamMessage(UUID conversationId, UUID userId, String userContent) {
        log.info("Starting streaming message in conversation: {} for user: {}", conversationId, userId);

        Conversation conversation = getConversation(conversationId, userId);
        User user = conversation.getUser();
        boolean isFirstExchange = conversation.getMessageCount() == 0;

        // Persist user message
        Message userMessage = new Message();
        userMessage.setConversation(conversation);
        userMessage.setRole(MessageRole.USER);
        userMessage.setContent(userContent);
        userMessage.setTokenCount(TokenCounter.estimateTokens(userContent));
        messageRepository.save(userMessage);

        // Build RAG context
        RagContext ragContext = buildRagContextSafely(userId, userContent);

        // Build context window
        String systemPrompt = systemPromptBuilder.build(user, "MyOffGridAI", ragContext);
        List<OllamaMessage> messages = contextWindowService.prepareMessages(
                conversationId, systemPrompt, userContent);

        final boolean hasRag = ragContext != null && ragContext.hasContext();

        StringBuilder contentAccumulator = new StringBuilder();
        StringBuilder thinkingAccumulator = new StringBuilder();

        return inferenceService.streamChatWithThinking(messages, userId)
                .map(chunk -> {
                    if (chunk.type() == ChunkType.THINKING) {
                        thinkingAccumulator.append(chunk.text());
                        return "{\"type\":\"thinking\",\"content\":" + jsonEscape(chunk.text()) + "}";
                    } else if (chunk.type() == ChunkType.CONTENT) {
                        contentAccumulator.append(chunk.text());
                        return "{\"type\":\"content\",\"content\":" + jsonEscape(chunk.text()) + "}";
                    } else {
                        // DONE chunk — persist and emit metadata
                        String fullResponse = contentAccumulator.toString();
                        String thinkingContent = thinkingAccumulator.length() > 0
                                ? thinkingAccumulator.toString() : null;

                        Message assistantMessage = new Message();
                        assistantMessage.setConversation(conversation);
                        assistantMessage.setRole(MessageRole.ASSISTANT);
                        assistantMessage.setContent(fullResponse);
                        assistantMessage.setHasRagContext(hasRag);
                        assistantMessage.setThinkingContent(thinkingContent);

                        if (chunk.metadata() != null) {
                            assistantMessage.setTokenCount(chunk.metadata().tokensGenerated());
                            assistantMessage.setTokensPerSecond(chunk.metadata().tokensPerSecond());
                            assistantMessage.setInferenceTimeSeconds(chunk.metadata().inferenceTimeSeconds());
                            assistantMessage.setStopReason(chunk.metadata().stopReason());
                            assistantMessage.setThinkingTokenCount(
                                    thinkingContent != null ? TokenCounter.estimateTokens(thinkingContent) : null);
                        }

                        messageRepository.save(assistantMessage);

                        conversation.setMessageCount(conversation.getMessageCount() + 2);
                        conversationRepository.save(conversation);

                        if (isFirstExchange) {
                            generateTitle(conversationId, userContent);
                        }

                        memoryExtractionService.extractAndStore(
                                userId, conversationId, userContent, fullResponse);

                        log.info("Streaming complete in conversation: {}", conversationId);

                        double thinkingTime = chunk.metadata() != null ? chunk.metadata().inferenceTimeSeconds() : 0;
                        double tokPerSec = chunk.metadata() != null ? chunk.metadata().tokensPerSecond() : 0;
                        int totalTokens = chunk.metadata() != null ? chunk.metadata().tokensGenerated() : 0;
                        String stopReason = chunk.metadata() != null ? chunk.metadata().stopReason() : "stop";
                        Integer thinkTokenCount = thinkingContent != null
                                ? TokenCounter.estimateTokens(thinkingContent) : null;

                        String thinkTokenField = thinkTokenCount != null
                                ? ",\"thinkingTokenCount\":" + thinkTokenCount : "";
                        return "{\"type\":\"done\",\"thinkingTime\":" + thinkingTime
                                + ",\"tokensPerSecond\":" + tokPerSec
                                + ",\"totalTokens\":" + totalTokens
                                + thinkTokenField
                                + ",\"stopReason\":" + jsonEscape(stopReason) + "}";
                    }
                });
    }

    /**
     * Edits a user message and triggers a new AI response.
     *
     * <p>Only USER role messages can be edited. Editing deletes all subsequent
     * messages and then re-triggers inference from the edited conversation history.</p>
     *
     * @param conversationId the conversation ID
     * @param messageId      the message to edit
     * @param userId         the user's ID
     * @param newContent     the updated message content
     * @return the edited message
     */
    @Transactional
    public Message editMessage(UUID conversationId, UUID messageId, UUID userId, String newContent) {
        log.info("Editing message: {} in conversation: {} for user: {}", messageId, conversationId, userId);

        Conversation conversation = getConversation(conversationId, userId);
        Message message = messageRepository.findById(messageId)
                .orElseThrow(() -> new EntityNotFoundException("Message not found: " + messageId));

        if (!message.getConversation().getId().equals(conversationId)) {
            throw new EntityNotFoundException("Message does not belong to this conversation");
        }

        if (message.getRole() != MessageRole.USER) {
            throw new IllegalArgumentException("Only user messages can be edited");
        }

        // Delete all messages after this one
        messageRepository.deleteMessagesAfter(conversationId, messageId);

        // Update the message content
        message.setContent(newContent);
        message.setTokenCount(TokenCounter.estimateTokens(newContent));
        messageRepository.save(message);

        // Recalculate message count
        long count = messageRepository.countByConversationId(conversationId);
        conversation.setMessageCount((int) count);
        conversationRepository.save(conversation);

        return message;
    }

    /**
     * Deletes a message and all subsequent messages in the conversation.
     *
     * @param conversationId the conversation ID
     * @param messageId      the message to delete (and all after it)
     * @param userId         the user's ID
     */
    @Transactional
    public void deleteMessage(UUID conversationId, UUID messageId, UUID userId) {
        log.info("Deleting message: {} in conversation: {} for user: {}", messageId, conversationId, userId);

        Conversation conversation = getConversation(conversationId, userId);
        Message message = messageRepository.findById(messageId)
                .orElseThrow(() -> new EntityNotFoundException("Message not found: " + messageId));

        if (!message.getConversation().getId().equals(conversationId)) {
            throw new EntityNotFoundException("Message does not belong to this conversation");
        }

        // Delete all messages after this one, then delete this one
        messageRepository.deleteMessagesAfter(conversationId, messageId);
        messageRepository.delete(message);

        // Recalculate message count
        long count = messageRepository.countByConversationId(conversationId);
        conversation.setMessageCount((int) count);
        conversationRepository.save(conversation);
    }

    /**
     * Branches a conversation at a specific message, creating a new independent conversation.
     *
     * <p>Copies all messages up to and including the specified message into a new
     * conversation. The new conversation is completely independent — future changes
     * do not affect the original.</p>
     *
     * @param conversationId the source conversation ID
     * @param messageId      the message to branch at (inclusive)
     * @param userId         the user's ID
     * @param title          optional title for the new conversation
     * @return the newly created branched conversation
     */
    @Transactional
    public Conversation branchConversation(UUID conversationId, UUID messageId, UUID userId, String title) {
        log.info("Branching conversation: {} at message: {} for user: {}", conversationId, messageId, userId);

        Conversation sourceConversation = getConversation(conversationId, userId);
        Message branchPoint = messageRepository.findById(messageId)
                .orElseThrow(() -> new EntityNotFoundException("Message not found: " + messageId));

        if (!branchPoint.getConversation().getId().equals(conversationId)) {
            throw new EntityNotFoundException("Message does not belong to this conversation");
        }

        // Create new conversation
        String branchTitle = title != null ? title
                : (sourceConversation.getTitle() != null ? sourceConversation.getTitle() + " (branch)" : "Branch");
        Conversation newConversation = createConversation(userId, branchTitle);

        // Copy messages up to and including the branch point
        List<Message> allMessages = messageRepository.findByConversationIdOrderByCreatedAtAsc(conversationId);
        int messageCount = 0;
        for (Message srcMsg : allMessages) {
            Message copy = new Message();
            copy.setConversation(newConversation);
            copy.setRole(srcMsg.getRole());
            copy.setContent(srcMsg.getContent());
            copy.setTokenCount(srcMsg.getTokenCount());
            copy.setHasRagContext(srcMsg.getHasRagContext());
            copy.setThinkingContent(srcMsg.getThinkingContent());
            copy.setTokensPerSecond(srcMsg.getTokensPerSecond());
            copy.setInferenceTimeSeconds(srcMsg.getInferenceTimeSeconds());
            copy.setStopReason(srcMsg.getStopReason());
            copy.setThinkingTokenCount(srcMsg.getThinkingTokenCount());
            messageRepository.save(copy);
            messageCount++;

            if (srcMsg.getId().equals(messageId)) {
                break;
            }
        }

        newConversation.setMessageCount(messageCount);
        return conversationRepository.save(newConversation);
    }

    /**
     * Regenerates the last assistant message by deleting it and re-running inference.
     *
     * @param conversationId the conversation ID
     * @param messageId      the assistant message to regenerate (must be the last assistant message)
     * @param userId         the user's ID
     * @return a Flux of typed JSON events (same format as streamMessage)
     */
    public Flux<String> regenerateMessage(UUID conversationId, UUID messageId, UUID userId) {
        log.info("Regenerating message: {} in conversation: {} for user: {}", messageId, conversationId, userId);

        Conversation conversation = getConversation(conversationId, userId);
        User user = conversation.getUser();

        Message message = messageRepository.findById(messageId)
                .orElseThrow(() -> new EntityNotFoundException("Message not found: " + messageId));

        if (!message.getConversation().getId().equals(conversationId)) {
            throw new EntityNotFoundException("Message does not belong to this conversation");
        }

        if (message.getRole() != MessageRole.ASSISTANT) {
            throw new IllegalArgumentException("Only assistant messages can be regenerated");
        }

        // Delete the assistant message
        messageRepository.delete(message);
        conversation.setMessageCount(conversation.getMessageCount() - 1);
        conversationRepository.save(conversation);

        // Find the last user message to use as context
        List<Message> remainingMessages = messageRepository.findByConversationIdOrderByCreatedAtAsc(conversationId);
        String lastUserContent = remainingMessages.stream()
                .filter(m -> m.getRole() == MessageRole.USER)
                .reduce((a, b) -> b)
                .map(Message::getContent)
                .orElse("");

        // Build context and stream new response
        RagContext ragContext = buildRagContextSafely(userId, lastUserContent);
        String systemPrompt = systemPromptBuilder.build(user, "MyOffGridAI", ragContext);
        List<OllamaMessage> contextMessages = contextWindowService.prepareMessages(
                conversationId, systemPrompt, lastUserContent);

        final boolean hasRag = ragContext != null && ragContext.hasContext();
        StringBuilder contentAccumulator = new StringBuilder();
        StringBuilder thinkingAccumulator = new StringBuilder();

        return inferenceService.streamChatWithThinking(contextMessages, userId)
                .map(chunk -> {
                    if (chunk.type() == ChunkType.THINKING) {
                        thinkingAccumulator.append(chunk.text());
                        return "{\"type\":\"thinking\",\"content\":" + jsonEscape(chunk.text()) + "}";
                    } else if (chunk.type() == ChunkType.CONTENT) {
                        contentAccumulator.append(chunk.text());
                        return "{\"type\":\"content\",\"content\":" + jsonEscape(chunk.text()) + "}";
                    } else {
                        String fullResponse = contentAccumulator.toString();
                        String thinkingContent = thinkingAccumulator.length() > 0
                                ? thinkingAccumulator.toString() : null;

                        Message assistantMessage = new Message();
                        assistantMessage.setConversation(conversation);
                        assistantMessage.setRole(MessageRole.ASSISTANT);
                        assistantMessage.setContent(fullResponse);
                        assistantMessage.setHasRagContext(hasRag);
                        assistantMessage.setThinkingContent(thinkingContent);

                        if (chunk.metadata() != null) {
                            assistantMessage.setTokenCount(chunk.metadata().tokensGenerated());
                            assistantMessage.setTokensPerSecond(chunk.metadata().tokensPerSecond());
                            assistantMessage.setInferenceTimeSeconds(chunk.metadata().inferenceTimeSeconds());
                            assistantMessage.setStopReason(chunk.metadata().stopReason());
                            assistantMessage.setThinkingTokenCount(
                                    thinkingContent != null ? TokenCounter.estimateTokens(thinkingContent) : null);
                        }

                        messageRepository.save(assistantMessage);
                        conversation.setMessageCount(conversation.getMessageCount() + 1);
                        conversationRepository.save(conversation);

                        double thinkingTime = chunk.metadata() != null ? chunk.metadata().inferenceTimeSeconds() : 0;
                        double tokPerSec = chunk.metadata() != null ? chunk.metadata().tokensPerSecond() : 0;
                        int totalTokens = chunk.metadata() != null ? chunk.metadata().tokensGenerated() : 0;
                        String stopReason = chunk.metadata() != null ? chunk.metadata().stopReason() : "stop";
                        Integer thinkTokenCount = thinkingContent != null
                                ? TokenCounter.estimateTokens(thinkingContent) : null;

                        String thinkTokenField = thinkTokenCount != null
                                ? ",\"thinkingTokenCount\":" + thinkTokenCount : "";
                        return "{\"type\":\"done\",\"thinkingTime\":" + thinkingTime
                                + ",\"tokensPerSecond\":" + tokPerSec
                                + ",\"totalTokens\":" + totalTokens
                                + thinkTokenField
                                + ",\"stopReason\":" + jsonEscape(stopReason) + "}";
                    }
                });
    }

    /**
     * Returns paginated messages for a conversation.
     *
     * @param userId         the user's ID
     * @param conversationId the conversation ID
     * @param pageable       pagination parameters
     * @return a page of messages
     */
    public Page<Message> getMessages(UUID userId, UUID conversationId, Pageable pageable) {
        getConversation(conversationId, userId); // ownership check
        return messageRepository.findByConversationIdOrderByCreatedAtAsc(conversationId, pageable);
    }

    /**
     * Searches conversations by title for the given user.
     *
     * @param userId   the user's ID
     * @param query    the search query to match against conversation titles
     * @param pageable pagination parameters
     * @return a page of conversations matching the query
     */
    public Page<Conversation> searchConversations(UUID userId, String query, Pageable pageable) {
        log.debug("Searching conversations for user: {} with query: '{}'", userId, query);
        return conversationRepository.findByUserIdAndTitleContainingIgnoreCaseOrderByUpdatedAtDesc(
                userId, query, pageable);
    }

    /**
     * Renames a conversation with a new title.
     *
     * @param conversationId the conversation ID
     * @param userId         the user's ID
     * @param newTitle       the new title
     * @return the updated conversation
     */
    @Transactional
    public Conversation renameConversation(UUID conversationId, UUID userId, String newTitle) {
        log.info("Renaming conversation: {} for user: {}", conversationId, userId);
        Conversation conversation = getConversation(conversationId, userId);
        conversation.setTitle(newTitle);
        return conversationRepository.save(conversation);
    }

    /**
     * Generates a title for a conversation based on the first user message.
     *
     * <p>Runs asynchronously so it does not block the chat response.
     * Calls Ollama with a title-generation prompt and updates the conversation.</p>
     *
     * @param conversationId   the conversation ID
     * @param firstUserMessage the first message in the conversation
     */
    @Async
    public void generateTitle(UUID conversationId, String firstUserMessage) {
        log.info("Generating title for conversation: {}", conversationId);
        try {
            String prompt = "Generate a 4-6 word title for a conversation that starts with: "
                    + firstUserMessage + ". Return only the title, no punctuation.";

            OllamaChatRequest request = new OllamaChatRequest(
                    systemConfigService.getAiSettings().modelName(),
                    List.of(new OllamaMessage("user", prompt)),
                    false,
                    Map.of("num_predict", AppConstants.TITLE_GENERATION_MAX_TOKENS),
                    false
            );

            OllamaChatResponse response = ollamaService.chat(request);
            String title = response.message().content().trim();

            conversationRepository.findById(conversationId).ifPresent(conversation -> {
                conversation.setTitle(title);
                conversationRepository.save(conversation);
                log.info("Generated title '{}' for conversation: {}", title, conversationId);
            });
        } catch (Exception e) {
            log.warn("Failed to generate title for conversation {}: {}", conversationId, e.getMessage());
        }
    }

    private RagContext buildRagContextSafely(UUID userId, String userContent) {
        try {
            return ragService.buildRagContext(userId, userContent);
        } catch (Exception e) {
            log.warn("Failed to build RAG context: {}. Proceeding without context.", e.getMessage());
            return null;
        }
    }

    /**
     * Escapes a string for safe inclusion in a JSON value.
     */
    private String jsonEscape(String value) {
        if (value == null) return "null";
        return "\"" + value
                .replace("\\", "\\\\")
                .replace("\"", "\\\"")
                .replace("\n", "\\n")
                .replace("\r", "\\r")
                .replace("\t", "\\t")
                + "\"";
    }
}
