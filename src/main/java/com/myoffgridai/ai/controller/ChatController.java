package com.myoffgridai.ai.controller;

import com.myoffgridai.ai.dto.*;
import com.myoffgridai.ai.model.Conversation;
import com.myoffgridai.ai.model.Message;
import com.myoffgridai.ai.repository.MessageRepository;
import com.myoffgridai.ai.service.ChatService;
import com.myoffgridai.auth.model.User;
import com.myoffgridai.common.response.ApiResponse;
import com.myoffgridai.config.AppConstants;
import jakarta.validation.Valid;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.util.List;
import java.util.UUID;

/**
 * REST controller for chat conversation and message management.
 *
 * <p>All endpoints require authentication. Users can only access their
 * own conversations. Supports both synchronous and streaming (SSE)
 * message responses.</p>
 */
@RestController
@RequestMapping(AppConstants.CHAT_API_PATH)
public class ChatController {

    private static final Logger log = LoggerFactory.getLogger(ChatController.class);

    private final ChatService chatService;
    private final MessageRepository messageRepository;

    /**
     * Constructs the chat controller.
     *
     * @param chatService       the chat service
     * @param messageRepository the message repository for message listing
     */
    public ChatController(ChatService chatService, MessageRepository messageRepository) {
        this.chatService = chatService;
        this.messageRepository = messageRepository;
    }

    /**
     * Creates a new conversation for the authenticated user.
     *
     * @param principal the authenticated user
     * @param request   optional title for the conversation
     * @return the created conversation
     */
    @PostMapping("/conversations")
    public ResponseEntity<ApiResponse<ConversationDto>> createConversation(
            @AuthenticationPrincipal User principal,
            @RequestBody(required = false) CreateConversationRequest request) {
        log.info("Creating conversation for user: {}", principal.getUsername());
        String title = request != null ? request.title() : null;
        Conversation conversation = chatService.createConversation(principal.getId(), title);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success(toConversationDto(conversation)));
    }

    /**
     * Lists conversations for the authenticated user.
     *
     * @param principal the authenticated user
     * @param page      page number (default 0)
     * @param size      page size (default 20)
     * @param archived  whether to include archived conversations
     * @return paginated list of conversation summaries
     */
    @GetMapping("/conversations")
    public ResponseEntity<ApiResponse<List<ConversationSummaryDto>>> listConversations(
            @AuthenticationPrincipal User principal,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(defaultValue = "false") boolean archived) {
        log.debug("Listing conversations for user: {}", principal.getUsername());
        int clampedSize = Math.min(size, AppConstants.MAX_PAGE_SIZE);
        Page<Conversation> conversations = chatService.getConversations(
                principal.getId(), archived, PageRequest.of(page, clampedSize));

        List<ConversationSummaryDto> summaries = conversations.getContent().stream()
                .map(this::toConversationSummaryDto)
                .toList();

        return ResponseEntity.ok(ApiResponse.paginated(
                summaries,
                conversations.getTotalElements(),
                conversations.getNumber(),
                conversations.getSize()));
    }

    /**
     * Gets a conversation with its messages.
     *
     * @param principal      the authenticated user
     * @param conversationId the conversation ID
     * @return the conversation details
     */
    @GetMapping("/conversations/{conversationId}")
    public ResponseEntity<ApiResponse<ConversationDto>> getConversation(
            @AuthenticationPrincipal User principal,
            @PathVariable UUID conversationId) {
        log.debug("Getting conversation: {} for user: {}", conversationId, principal.getUsername());
        Conversation conversation = chatService.getConversation(conversationId, principal.getId());
        return ResponseEntity.ok(ApiResponse.success(toConversationDto(conversation)));
    }

    /**
     * Deletes a conversation and all its messages.
     *
     * @param principal      the authenticated user
     * @param conversationId the conversation ID
     * @return empty success response
     */
    @DeleteMapping("/conversations/{conversationId}")
    public ResponseEntity<ApiResponse<Void>> deleteConversation(
            @AuthenticationPrincipal User principal,
            @PathVariable UUID conversationId) {
        log.info("Deleting conversation: {} for user: {}", conversationId, principal.getUsername());
        chatService.deleteConversation(conversationId, principal.getId());
        return ResponseEntity.ok(ApiResponse.success(null, "Conversation deleted"));
    }

    /**
     * Archives a conversation.
     *
     * @param principal      the authenticated user
     * @param conversationId the conversation ID
     * @return empty success response
     */
    @PutMapping("/conversations/{conversationId}/archive")
    public ResponseEntity<ApiResponse<Void>> archiveConversation(
            @AuthenticationPrincipal User principal,
            @PathVariable UUID conversationId) {
        log.info("Archiving conversation: {} for user: {}", conversationId, principal.getUsername());
        chatService.archiveConversation(conversationId, principal.getId());
        return ResponseEntity.ok(ApiResponse.success(null, "Conversation archived"));
    }

    /**
     * Sends a message in a conversation. Supports synchronous and streaming modes.
     *
     * <p>If {@code stream=false}, returns a standard JSON response with the assistant's
     * message. If {@code stream=true}, returns an SSE stream of token chunks,
     * terminating with a {@code [DONE]} event.</p>
     *
     * @param principal      the authenticated user
     * @param conversationId the conversation ID
     * @param request        the message request with content and stream flag
     * @return either a JSON response or an SSE stream
     */
    @PostMapping("/conversations/{conversationId}/messages")
    public ResponseEntity<?> sendMessage(
            @AuthenticationPrincipal User principal,
            @PathVariable UUID conversationId,
            @Valid @RequestBody SendMessageRequest request) {
        log.info("Message in conversation: {} for user: {}, stream: {}",
                conversationId, principal.getUsername(), request.stream());

        if (request.stream()) {
            SseEmitter emitter = new SseEmitter(AppConstants.OLLAMA_READ_TIMEOUT_SECONDS * 1000L);

            chatService.streamMessage(conversationId, principal.getId(), request.content())
                    .subscribe(
                            token -> {
                                try {
                                    emitter.send(SseEmitter.event().data(token));
                                } catch (Exception e) {
                                    emitter.completeWithError(e);
                                }
                            },
                            emitter::completeWithError,
                            () -> {
                                try {
                                    emitter.send(SseEmitter.event().data("[DONE]"));
                                    emitter.complete();
                                } catch (Exception e) {
                                    emitter.completeWithError(e);
                                }
                            }
                    );

            return ResponseEntity.ok()
                    .contentType(MediaType.TEXT_EVENT_STREAM)
                    .body(emitter);
        }

        Message assistantMessage = chatService.sendMessage(
                conversationId, principal.getId(), request.content());
        return ResponseEntity.ok(ApiResponse.success(toMessageDto(assistantMessage)));
    }

    /**
     * Lists messages in a conversation with pagination.
     *
     * @param principal      the authenticated user
     * @param conversationId the conversation ID
     * @param page           page number (default 0)
     * @param size           page size (default 20)
     * @return paginated list of messages
     */
    @GetMapping("/conversations/{conversationId}/messages")
    public ResponseEntity<ApiResponse<List<MessageDto>>> listMessages(
            @AuthenticationPrincipal User principal,
            @PathVariable UUID conversationId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        log.debug("Listing messages for conversation: {}", conversationId);
        // Verify ownership
        chatService.getConversation(conversationId, principal.getId());

        int clampedSize = Math.min(size, AppConstants.MAX_PAGE_SIZE);
        Page<Message> messages = messageRepository.findByConversationIdOrderByCreatedAtAsc(
                conversationId, PageRequest.of(page, clampedSize));

        List<MessageDto> dtos = messages.getContent().stream()
                .map(this::toMessageDto)
                .toList();

        return ResponseEntity.ok(ApiResponse.paginated(
                dtos,
                messages.getTotalElements(),
                messages.getNumber(),
                messages.getSize()));
    }

    private ConversationDto toConversationDto(Conversation c) {
        return new ConversationDto(
                c.getId(), c.getTitle(), c.getIsArchived(),
                c.getMessageCount(), c.getCreatedAt(), c.getUpdatedAt());
    }

    private ConversationSummaryDto toConversationSummaryDto(Conversation c) {
        return new ConversationSummaryDto(
                c.getId(), c.getTitle(), c.getIsArchived(),
                c.getMessageCount(), c.getUpdatedAt(), null);
    }

    private MessageDto toMessageDto(Message m) {
        return new MessageDto(
                m.getId(), m.getRole(), m.getContent(),
                m.getTokenCount(), m.getHasRagContext(), m.getCreatedAt());
    }
}
