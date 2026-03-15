package com.myoffgridai.ai.service;

import com.myoffgridai.ai.dto.OllamaChatChunk;
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
                       SystemPromptBuilder systemPromptBuilder,
                       ContextWindowService contextWindowService,
                       RagService ragService,
                       MemoryExtractionService memoryExtractionService,
                       SystemConfigService systemConfigService) {
        this.conversationRepository = conversationRepository;
        this.messageRepository = messageRepository;
        this.userRepository = userRepository;
        this.ollamaService = ollamaService;
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
     * Sends a message and returns a streaming response as a Flux of token strings.
     *
     * <p>Persists the user message, builds RAG context, builds the context window,
     * calls Ollama in streaming mode, and persists the complete response after the
     * stream ends. Triggers async memory extraction after completion.</p>
     *
     * @param conversationId the conversation ID
     * @param userId         the user's ID
     * @param userContent    the user's message content
     * @return a Flux emitting token strings as they arrive
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

        // Call Ollama streaming with dynamic model, temperature, and context size
        AiSettingsDto streamSettings = systemConfigService.getAiSettings();
        OllamaChatRequest request = new OllamaChatRequest(
                streamSettings.modelName(), messages, true,
                Map.of("temperature", streamSettings.temperature(), "num_ctx", streamSettings.contextSize()));

        final boolean hasRag = ragContext != null && ragContext.hasContext();

        return ollamaService.chatStream(request)
                .map(chunk -> chunk.message() != null ? chunk.message().content() : "")
                .collectList()
                .flatMapMany(tokens -> {
                    String fullResponse = String.join("", tokens);

                    // Persist assistant message
                    Message assistantMessage = new Message();
                    assistantMessage.setConversation(conversation);
                    assistantMessage.setRole(MessageRole.ASSISTANT);
                    assistantMessage.setContent(fullResponse);
                    assistantMessage.setTokenCount(TokenCounter.estimateTokens(fullResponse));
                    assistantMessage.setHasRagContext(hasRag);
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
                            userId, conversationId, userContent, fullResponse);

                    log.info("Streaming complete in conversation: {}", conversationId);
                    return Flux.fromIterable(tokens);
                });
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
}
