package com.myoffgridai.ai.service;

import com.myoffgridai.ai.SourceTag;
import com.myoffgridai.ai.dto.ChunkType;
import com.myoffgridai.ai.dto.InferenceChunk;
import com.myoffgridai.ai.dto.InferenceMetadata;
import com.myoffgridai.ai.dto.OllamaChatChunk;
import com.myoffgridai.ai.dto.OllamaChatResponse;
import com.myoffgridai.ai.dto.OllamaMessage;
import com.myoffgridai.ai.judge.JudgeInferenceService;
import com.myoffgridai.ai.judge.JudgeResult;
import com.myoffgridai.ai.model.Conversation;
import com.myoffgridai.ai.model.Message;
import com.myoffgridai.ai.model.MessageRole;
import com.myoffgridai.ai.repository.ConversationRepository;
import com.myoffgridai.ai.repository.MessageRepository;
import com.myoffgridai.auth.model.Role;
import com.myoffgridai.auth.model.User;
import com.myoffgridai.auth.repository.UserRepository;
import com.myoffgridai.common.exception.EntityNotFoundException;
import com.myoffgridai.frontier.FrontierApiRouter;
import com.myoffgridai.frontier.FrontierProvider;
import com.myoffgridai.memory.service.MemoryExtractionService;
import com.myoffgridai.memory.service.RagService;
import com.myoffgridai.settings.dto.ExternalApiSettingsDto;
import com.myoffgridai.settings.service.ExternalApiSettingsService;
import com.myoffgridai.system.dto.AiSettingsDto;
import com.myoffgridai.system.service.SystemConfigService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import reactor.core.publisher.Flux;
import reactor.test.StepVerifier;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * Unit tests for {@link ChatService}.
 *
 * <p>Validates conversation lifecycle (create, list, archive, delete, rename, search),
 * synchronous and streaming message exchange, and the P11 message editing, deletion,
 * branching, and regeneration operations.</p>
 */
@ExtendWith(MockitoExtension.class)
class ChatServiceTest {

    @Mock private ConversationRepository conversationRepository;
    @Mock private MessageRepository messageRepository;
    @Mock private UserRepository userRepository;
    @Mock private OllamaService ollamaService;
    @Mock private InferenceService inferenceService;
    @Mock private SystemPromptBuilder systemPromptBuilder;
    @Mock private ContextWindowService contextWindowService;
    @Mock private RagService ragService;
    @Mock private MemoryExtractionService memoryExtractionService;
    @Mock private SystemConfigService systemConfigService;
    @Mock private JudgeInferenceService judgeInferenceService;
    @Mock private FrontierApiRouter frontierApiRouter;
    @Mock private ExternalApiSettingsService externalApiSettingsService;

    @InjectMocks
    private ChatService chatService;

    private User testUser;
    private UUID userId;
    private UUID conversationId;
    private Conversation testConversation;

    @BeforeEach
    void setUp() {
        userId = UUID.randomUUID();
        conversationId = UUID.randomUUID();

        testUser = new User();
        testUser.setId(userId);
        testUser.setUsername("testuser");
        testUser.setDisplayName("Test User");
        testUser.setRole(Role.ROLE_MEMBER);

        testConversation = new Conversation();
        testConversation.setId(conversationId);
        testConversation.setUser(testUser);
        testConversation.setMessageCount(0);
        testConversation.setCreatedAt(Instant.now());
        testConversation.setUpdatedAt(Instant.now());

        // Default AI settings for all tests that call sendMessage/streamMessage
        lenient().when(systemConfigService.getAiSettings())
                .thenReturn(new AiSettingsDto("test-model", 0.7, 0.45, 5, 2048, 4096, 20));

        // Default external API settings (judge disabled)
        lenient().when(externalApiSettingsService.getSettings())
                .thenReturn(new ExternalApiSettingsDto(
                        false, "claude-sonnet-4-20250514", false,
                        false, false, 512, 5,
                        false, false,
                        false, false,
                        false, false,
                        FrontierProvider.CLAUDE,
                        false, null, 7.5
                ));
    }

    // ── createConversation tests ─────────────────────────────────────────

    @Test
    void createConversation_success() {
        when(userRepository.findById(userId)).thenReturn(Optional.of(testUser));
        when(conversationRepository.save(any(Conversation.class))).thenReturn(testConversation);

        Conversation result = chatService.createConversation(userId, "Test Title");
        assertNotNull(result);
        verify(conversationRepository).save(any(Conversation.class));
    }

    @Test
    void createConversation_userNotFound_throws() {
        when(userRepository.findById(userId)).thenReturn(Optional.empty());
        assertThrows(EntityNotFoundException.class,
                () -> chatService.createConversation(userId, null));
    }

    // ── getConversation tests ────────────────────────────────────────────

    @Test
    void getConversation_success() {
        when(conversationRepository.findByIdAndUserId(conversationId, userId))
                .thenReturn(Optional.of(testConversation));

        Conversation result = chatService.getConversation(conversationId, userId);
        assertEquals(conversationId, result.getId());
    }

    @Test
    void getConversation_notOwned_throws() {
        UUID otherUserId = UUID.randomUUID();
        when(conversationRepository.findByIdAndUserId(conversationId, otherUserId))
                .thenReturn(Optional.empty());

        assertThrows(EntityNotFoundException.class,
                () -> chatService.getConversation(conversationId, otherUserId));
    }

    // ── sendMessage tests ────────────────────────────────────────────────

    @Test
    void sendMessage_persistsUserAndAssistantMessages() {
        when(conversationRepository.findByIdAndUserId(conversationId, userId))
                .thenReturn(Optional.of(testConversation));
        when(systemPromptBuilder.build(any(User.class), anyString(), any())).thenReturn("system prompt");
        when(contextWindowService.prepareMessages(any(), anyString(), anyString()))
                .thenReturn(List.of(new OllamaMessage("system", "prompt"),
                        new OllamaMessage("user", "hello")));

        OllamaChatResponse ollamaResponse = new OllamaChatResponse(
                new OllamaMessage("assistant", "Hi there!"), true, 1000L, 5);
        when(ollamaService.chat(any())).thenReturn(ollamaResponse);
        when(messageRepository.save(any(Message.class))).thenAnswer(inv -> {
            Message m = inv.getArgument(0);
            m.setId(UUID.randomUUID());
            return m;
        });
        when(conversationRepository.save(any(Conversation.class))).thenReturn(testConversation);

        Message result = chatService.sendMessage(conversationId, userId, "hello");

        assertNotNull(result);
        assertEquals(MessageRole.ASSISTANT, result.getRole());
        assertEquals("Hi there!", result.getContent());

        // Should save user message + assistant message = 2 calls
        verify(messageRepository, times(2)).save(any(Message.class));
    }

    @Test
    void sendMessage_incrementsMessageCount() {
        testConversation.setMessageCount(2);
        when(conversationRepository.findByIdAndUserId(conversationId, userId))
                .thenReturn(Optional.of(testConversation));
        when(systemPromptBuilder.build(any(User.class), anyString(), any())).thenReturn("prompt");
        when(contextWindowService.prepareMessages(any(), anyString(), anyString()))
                .thenReturn(List.of(new OllamaMessage("user", "hello")));

        OllamaChatResponse ollamaResponse = new OllamaChatResponse(
                new OllamaMessage("assistant", "response"), true, 1000L, 5);
        when(ollamaService.chat(any())).thenReturn(ollamaResponse);
        when(messageRepository.save(any(Message.class))).thenAnswer(inv -> inv.getArgument(0));
        when(conversationRepository.save(any(Conversation.class))).thenReturn(testConversation);

        chatService.sendMessage(conversationId, userId, "hello");

        ArgumentCaptor<Conversation> captor = ArgumentCaptor.forClass(Conversation.class);
        verify(conversationRepository).save(captor.capture());
        assertEquals(4, captor.getValue().getMessageCount());
    }

    // ── streamMessage tests ──────────────────────────────────────────────

    @Test
    void streamMessage_emitsTokens() {
        when(conversationRepository.findByIdAndUserId(conversationId, userId))
                .thenReturn(Optional.of(testConversation));
        when(systemPromptBuilder.build(any(User.class), anyString(), any())).thenReturn("prompt");
        when(contextWindowService.prepareMessages(any(), anyString(), anyString()))
                .thenReturn(List.of(new OllamaMessage("user", "hello")));

        // streamMessage now delegates to inferenceService.streamChatWithThinking
        InferenceChunk contentChunk1 = new InferenceChunk(ChunkType.CONTENT, "Hi", null);
        InferenceChunk contentChunk2 = new InferenceChunk(ChunkType.CONTENT, " there", null);
        InferenceChunk doneChunk = new InferenceChunk(ChunkType.DONE, null,
                new InferenceMetadata(2, 5.0, 1.0, "stop"));
        when(inferenceService.streamChatWithThinking(any(), any()))
                .thenReturn(Flux.just(contentChunk1, contentChunk2, doneChunk));
        when(messageRepository.save(any(Message.class))).thenAnswer(inv -> inv.getArgument(0));
        when(conversationRepository.save(any(Conversation.class))).thenReturn(testConversation);

        Flux<String> result = chatService.streamMessage(conversationId, userId, "hello");

        java.util.List<String> events = new java.util.ArrayList<>();
        StepVerifier.create(result)
                .recordWith(() -> events)
                .thenConsumeWhile(e -> true)
                .verifyComplete();

        assertEquals(3, events.size());
        assertTrue(events.get(0).contains("\"type\":\"content\""));
        assertTrue(events.get(0).contains("Hi"));
        assertTrue(events.get(1).contains("\"type\":\"content\""));
        assertTrue(events.get(1).contains(" there"));
        assertTrue(events.get(2).contains("\"type\":\"done\""));
    }

    @Test
    void streamMessage_setsThinkingTokenCount_whenThinkingPresent() {
        when(conversationRepository.findByIdAndUserId(conversationId, userId))
                .thenReturn(Optional.of(testConversation));
        when(systemPromptBuilder.build(any(User.class), anyString(), any())).thenReturn("prompt");
        when(contextWindowService.prepareMessages(any(), anyString(), anyString()))
                .thenReturn(List.of(new OllamaMessage("user", "hello")));

        InferenceChunk thinkChunk = new InferenceChunk(ChunkType.THINKING, "Let me reason about this", null);
        InferenceChunk contentChunk = new InferenceChunk(ChunkType.CONTENT, "response", null);
        InferenceChunk doneChunk = new InferenceChunk(ChunkType.DONE, null,
                new InferenceMetadata(10, 5.0, 2.0, "stop"));
        when(inferenceService.streamChatWithThinking(any(), any()))
                .thenReturn(Flux.just(thinkChunk, contentChunk, doneChunk));
        when(messageRepository.save(any(Message.class))).thenAnswer(inv -> inv.getArgument(0));
        when(conversationRepository.save(any(Conversation.class))).thenReturn(testConversation);

        Flux<String> result = chatService.streamMessage(conversationId, userId, "hello");

        List<String> events = new java.util.ArrayList<>();
        StepVerifier.create(result)
                .recordWith(() -> events)
                .thenConsumeWhile(e -> true)
                .verifyComplete();

        // Verify thinking token count is in the done event
        assertTrue(events.get(2).contains("\"thinkingTokenCount\":"));

        // Verify message saved with thinkingTokenCount set
        ArgumentCaptor<Message> msgCaptor = ArgumentCaptor.forClass(Message.class);
        verify(messageRepository, atLeast(2)).save(msgCaptor.capture());
        Message assistantMsg = msgCaptor.getAllValues().stream()
                .filter(m -> m.getRole() == MessageRole.ASSISTANT)
                .findFirst().orElseThrow();
        assertNotNull(assistantMsg.getThinkingTokenCount());
        assertTrue(assistantMsg.getThinkingTokenCount() > 0);
        assertEquals("Let me reason about this", assistantMsg.getThinkingContent());
    }

    // ── deleteConversation tests ─────────────────────────────────────────

    @Test
    void deleteConversation_deletesMessagesAndConversation() {
        when(conversationRepository.findByIdAndUserId(conversationId, userId))
                .thenReturn(Optional.of(testConversation));

        chatService.deleteConversation(conversationId, userId);

        verify(messageRepository).deleteByConversationId(conversationId);
        verify(conversationRepository).delete(testConversation);
    }

    // ── archiveConversation tests ────────────────────────────────────────

    @Test
    void archiveConversation_setsArchived() {
        when(conversationRepository.findByIdAndUserId(conversationId, userId))
                .thenReturn(Optional.of(testConversation));
        when(conversationRepository.save(any())).thenReturn(testConversation);

        chatService.archiveConversation(conversationId, userId);

        assertTrue(testConversation.getIsArchived());
        verify(conversationRepository).save(testConversation);
    }

    // ── searchConversations tests ──────────────────────────────────────

    @Test
    void searchConversations_returnsMatchingConversations() {
        testConversation.setTitle("Solar Panel Setup");
        Page<Conversation> page = new PageImpl<>(List.of(testConversation));
        when(conversationRepository.findByUserIdAndTitleContainingIgnoreCaseOrderByUpdatedAtDesc(
                eq(userId), eq("Solar"), any()))
                .thenReturn(page);

        Page<Conversation> result = chatService.searchConversations(
                userId, "Solar", PageRequest.of(0, 20));

        assertEquals(1, result.getTotalElements());
        assertEquals("Solar Panel Setup", result.getContent().get(0).getTitle());
    }

    @Test
    void searchConversations_emptyQuery_returnsEmptyPage() {
        Page<Conversation> emptyPage = new PageImpl<>(List.of());
        when(conversationRepository.findByUserIdAndTitleContainingIgnoreCaseOrderByUpdatedAtDesc(
                eq(userId), eq(""), any()))
                .thenReturn(emptyPage);

        Page<Conversation> result = chatService.searchConversations(
                userId, "", PageRequest.of(0, 20));

        assertEquals(0, result.getTotalElements());
    }

    // ── renameConversation tests ────────────────────────────────────────

    @Test
    void renameConversation_updatesTitle() {
        when(conversationRepository.findByIdAndUserId(conversationId, userId))
                .thenReturn(Optional.of(testConversation));
        when(conversationRepository.save(any(Conversation.class))).thenReturn(testConversation);

        Conversation result = chatService.renameConversation(conversationId, userId, "New Title");

        assertEquals("New Title", result.getTitle());
        verify(conversationRepository).save(testConversation);
    }

    @Test
    void renameConversation_notOwned_throws() {
        UUID otherUserId = UUID.randomUUID();
        when(conversationRepository.findByIdAndUserId(conversationId, otherUserId))
                .thenReturn(Optional.empty());

        assertThrows(EntityNotFoundException.class,
                () -> chatService.renameConversation(conversationId, otherUserId, "New Title"));
    }

    // ── generateTitle tests ──────────────────────────────────────────────

    @Test
    void generateTitle_updatesConversationTitle() {
        OllamaChatResponse titleResponse = new OllamaChatResponse(
                new OllamaMessage("assistant", "Getting Started Guide"), true, 500L, 3);
        when(ollamaService.chat(any())).thenReturn(titleResponse);
        when(conversationRepository.findById(conversationId)).thenReturn(Optional.of(testConversation));
        when(conversationRepository.save(any())).thenReturn(testConversation);

        chatService.generateTitle(conversationId, "How do I get started?");

        assertEquals("Getting Started Guide", testConversation.getTitle());
        verify(conversationRepository).save(testConversation);
    }

    @Test
    void generateTitle_handlesOllamaFailure_gracefully() {
        when(ollamaService.chat(any())).thenThrow(new RuntimeException("Ollama down"));

        // Should not throw
        assertDoesNotThrow(() ->
                chatService.generateTitle(conversationId, "test message"));
    }

    // ── editMessage tests ────────────────────────────────────────────────

    @Test
    void editMessage_updatesContentAndDeletesSubsequent() {
        // Set up conversation ownership
        when(conversationRepository.findByIdAndUserId(conversationId, userId))
                .thenReturn(Optional.of(testConversation));

        // Create a user message
        UUID messageId = UUID.randomUUID();
        Message userMessage = new Message();
        userMessage.setId(messageId);
        userMessage.setConversation(testConversation);
        userMessage.setRole(MessageRole.USER);
        userMessage.setContent("original content");
        userMessage.setCreatedAt(Instant.now());

        when(messageRepository.findById(messageId)).thenReturn(Optional.of(userMessage));
        when(messageRepository.countByConversationId(conversationId)).thenReturn(1L);
        when(messageRepository.save(any(Message.class))).thenAnswer(inv -> inv.getArgument(0));
        when(conversationRepository.save(any(Conversation.class))).thenReturn(testConversation);

        Message result = chatService.editMessage(conversationId, messageId, userId, "updated content");

        assertEquals("updated content", result.getContent());
        verify(messageRepository).deleteMessagesAfter(conversationId, messageId);
        verify(messageRepository).save(userMessage);

        // Verify message count recalculation
        ArgumentCaptor<Conversation> captor = ArgumentCaptor.forClass(Conversation.class);
        verify(conversationRepository).save(captor.capture());
        assertEquals(1, captor.getValue().getMessageCount());
    }

    @Test
    void editMessage_throwsForNonUserMessage() {
        when(conversationRepository.findByIdAndUserId(conversationId, userId))
                .thenReturn(Optional.of(testConversation));

        UUID messageId = UUID.randomUUID();
        Message assistantMessage = new Message();
        assistantMessage.setId(messageId);
        assistantMessage.setConversation(testConversation);
        assistantMessage.setRole(MessageRole.ASSISTANT);
        assistantMessage.setContent("assistant response");

        when(messageRepository.findById(messageId)).thenReturn(Optional.of(assistantMessage));

        assertThrows(IllegalArgumentException.class,
                () -> chatService.editMessage(conversationId, messageId, userId, "new content"));
    }

    @Test
    void editMessage_throwsForMessageNotInConversation() {
        when(conversationRepository.findByIdAndUserId(conversationId, userId))
                .thenReturn(Optional.of(testConversation));

        UUID messageId = UUID.randomUUID();
        Conversation otherConversation = new Conversation();
        otherConversation.setId(UUID.randomUUID());

        Message message = new Message();
        message.setId(messageId);
        message.setConversation(otherConversation);
        message.setRole(MessageRole.USER);

        when(messageRepository.findById(messageId)).thenReturn(Optional.of(message));

        assertThrows(EntityNotFoundException.class,
                () -> chatService.editMessage(conversationId, messageId, userId, "new content"));
    }

    @Test
    void editMessage_throwsForNonExistentMessage() {
        when(conversationRepository.findByIdAndUserId(conversationId, userId))
                .thenReturn(Optional.of(testConversation));

        UUID messageId = UUID.randomUUID();
        when(messageRepository.findById(messageId)).thenReturn(Optional.empty());

        assertThrows(EntityNotFoundException.class,
                () -> chatService.editMessage(conversationId, messageId, userId, "new content"));
    }

    // ── deleteMessage tests ──────────────────────────────────────────────

    @Test
    void deleteMessage_deletesMessageAndSubsequent() {
        when(conversationRepository.findByIdAndUserId(conversationId, userId))
                .thenReturn(Optional.of(testConversation));

        UUID messageId = UUID.randomUUID();
        Message message = new Message();
        message.setId(messageId);
        message.setConversation(testConversation);
        message.setRole(MessageRole.USER);
        message.setContent("to be deleted");

        when(messageRepository.findById(messageId)).thenReturn(Optional.of(message));
        when(messageRepository.countByConversationId(conversationId)).thenReturn(0L);
        when(conversationRepository.save(any(Conversation.class))).thenReturn(testConversation);

        chatService.deleteMessage(conversationId, messageId, userId);

        verify(messageRepository).deleteMessagesAfter(conversationId, messageId);
        verify(messageRepository).delete(message);

        ArgumentCaptor<Conversation> captor = ArgumentCaptor.forClass(Conversation.class);
        verify(conversationRepository).save(captor.capture());
        assertEquals(0, captor.getValue().getMessageCount());
    }

    @Test
    void deleteMessage_throwsForMessageNotInConversation() {
        when(conversationRepository.findByIdAndUserId(conversationId, userId))
                .thenReturn(Optional.of(testConversation));

        UUID messageId = UUID.randomUUID();
        Conversation otherConversation = new Conversation();
        otherConversation.setId(UUID.randomUUID());

        Message message = new Message();
        message.setId(messageId);
        message.setConversation(otherConversation);

        when(messageRepository.findById(messageId)).thenReturn(Optional.of(message));

        assertThrows(EntityNotFoundException.class,
                () -> chatService.deleteMessage(conversationId, messageId, userId));
    }

    @Test
    void deleteMessage_throwsForNonExistentMessage() {
        when(conversationRepository.findByIdAndUserId(conversationId, userId))
                .thenReturn(Optional.of(testConversation));

        UUID messageId = UUID.randomUUID();
        when(messageRepository.findById(messageId)).thenReturn(Optional.empty());

        assertThrows(EntityNotFoundException.class,
                () -> chatService.deleteMessage(conversationId, messageId, userId));
    }

    // ── branchConversation tests ─────────────────────────────────────────

    @Test
    void branchConversation_copiesMessagesUpToPoint() {
        testConversation.setTitle("Original Title");
        when(conversationRepository.findByIdAndUserId(conversationId, userId))
                .thenReturn(Optional.of(testConversation));

        // Set up branch point message
        UUID branchMessageId = UUID.randomUUID();
        Message msg1 = new Message();
        msg1.setId(UUID.randomUUID());
        msg1.setConversation(testConversation);
        msg1.setRole(MessageRole.USER);
        msg1.setContent("first message");
        msg1.setTokenCount(5);
        msg1.setHasRagContext(false);
        msg1.setCreatedAt(Instant.now());

        Message msg2 = new Message();
        msg2.setId(branchMessageId);
        msg2.setConversation(testConversation);
        msg2.setRole(MessageRole.ASSISTANT);
        msg2.setContent("second message");
        msg2.setTokenCount(5);
        msg2.setHasRagContext(false);
        msg2.setCreatedAt(Instant.now());

        Message msg3 = new Message();
        msg3.setId(UUID.randomUUID());
        msg3.setConversation(testConversation);
        msg3.setRole(MessageRole.USER);
        msg3.setContent("third message - should not be copied");
        msg3.setTokenCount(8);
        msg3.setHasRagContext(false);
        msg3.setCreatedAt(Instant.now());

        when(messageRepository.findById(branchMessageId)).thenReturn(Optional.of(msg2));
        when(messageRepository.findByConversationIdOrderByCreatedAtAsc(conversationId))
                .thenReturn(List.of(msg1, msg2, msg3));

        // For createConversation called internally
        when(userRepository.findById(userId)).thenReturn(Optional.of(testUser));

        Conversation branchedConversation = new Conversation();
        branchedConversation.setId(UUID.randomUUID());
        branchedConversation.setUser(testUser);
        branchedConversation.setTitle("Original Title (branch)");

        // The first save is from createConversation, the final save updates messageCount
        when(conversationRepository.save(any(Conversation.class)))
                .thenReturn(branchedConversation);
        when(messageRepository.save(any(Message.class))).thenAnswer(inv -> {
            Message m = inv.getArgument(0);
            m.setId(UUID.randomUUID());
            return m;
        });

        Conversation result = chatService.branchConversation(
                conversationId, branchMessageId, userId, null);

        assertNotNull(result);

        // Should save exactly 2 messages (msg1 and msg2, stopping at branchMessageId)
        verify(messageRepository, times(2)).save(argThat(m ->
                m.getContent().equals("first message") || m.getContent().equals("second message")));

        // msg3 should NOT have been copied
        verify(messageRepository, never()).save(argThat(m ->
                m.getContent() != null && m.getContent().contains("third message")));
    }

    @Test
    void branchConversation_usesCustomTitle() {
        testConversation.setTitle("Original");
        when(conversationRepository.findByIdAndUserId(conversationId, userId))
                .thenReturn(Optional.of(testConversation));

        UUID branchMessageId = UUID.randomUUID();
        Message msg = new Message();
        msg.setId(branchMessageId);
        msg.setConversation(testConversation);
        msg.setRole(MessageRole.USER);
        msg.setContent("only message");
        msg.setTokenCount(2);
        msg.setHasRagContext(false);
        msg.setCreatedAt(Instant.now());

        when(messageRepository.findById(branchMessageId)).thenReturn(Optional.of(msg));
        when(messageRepository.findByConversationIdOrderByCreatedAtAsc(conversationId))
                .thenReturn(List.of(msg));
        when(userRepository.findById(userId)).thenReturn(Optional.of(testUser));
        when(conversationRepository.save(any(Conversation.class))).thenAnswer(inv -> inv.getArgument(0));
        when(messageRepository.save(any(Message.class))).thenAnswer(inv -> inv.getArgument(0));

        // Capture the title passed to createConversation
        chatService.branchConversation(conversationId, branchMessageId, userId, "Custom Branch Title");

        // The first conversationRepository.save is from createConversation with the custom title
        ArgumentCaptor<Conversation> captor = ArgumentCaptor.forClass(Conversation.class);
        verify(conversationRepository, atLeastOnce()).save(captor.capture());
        assertTrue(captor.getAllValues().stream()
                .anyMatch(c -> "Custom Branch Title".equals(c.getTitle())));
    }

    @Test
    void branchConversation_throwsForMessageNotInConversation() {
        when(conversationRepository.findByIdAndUserId(conversationId, userId))
                .thenReturn(Optional.of(testConversation));

        UUID messageId = UUID.randomUUID();
        Conversation otherConversation = new Conversation();
        otherConversation.setId(UUID.randomUUID());

        Message message = new Message();
        message.setId(messageId);
        message.setConversation(otherConversation);

        when(messageRepository.findById(messageId)).thenReturn(Optional.of(message));

        assertThrows(EntityNotFoundException.class,
                () -> chatService.branchConversation(conversationId, messageId, userId, null));
    }

    // ── regenerateMessage tests ──────────────────────────────────────────

    @Test
    void regenerateMessage_deletesAndRestreams() {
        testConversation.setMessageCount(4);
        when(conversationRepository.findByIdAndUserId(conversationId, userId))
                .thenReturn(Optional.of(testConversation));

        UUID assistantMsgId = UUID.randomUUID();
        Message assistantMsg = new Message();
        assistantMsg.setId(assistantMsgId);
        assistantMsg.setConversation(testConversation);
        assistantMsg.setRole(MessageRole.ASSISTANT);
        assistantMsg.setContent("old response");

        when(messageRepository.findById(assistantMsgId)).thenReturn(Optional.of(assistantMsg));

        // After deleting the assistant message, remaining messages
        Message userMsg = new Message();
        userMsg.setId(UUID.randomUUID());
        userMsg.setConversation(testConversation);
        userMsg.setRole(MessageRole.USER);
        userMsg.setContent("user question");
        userMsg.setCreatedAt(Instant.now());

        when(messageRepository.findByConversationIdOrderByCreatedAtAsc(conversationId))
                .thenReturn(List.of(userMsg));

        when(systemPromptBuilder.build(any(User.class), anyString(), any())).thenReturn("prompt");
        when(contextWindowService.prepareMessages(any(), anyString(), anyString()))
                .thenReturn(List.of(new OllamaMessage("user", "user question")));

        // Simulate inference producing content + done
        InferenceChunk contentChunk = new InferenceChunk(ChunkType.CONTENT, "new response", null);
        InferenceChunk doneChunk = new InferenceChunk(ChunkType.DONE, null,
                new InferenceMetadata(10, 5.0, 2.0, "stop"));
        when(inferenceService.streamChatWithThinking(any(), any()))
                .thenReturn(Flux.just(contentChunk, doneChunk));

        when(messageRepository.save(any(Message.class))).thenAnswer(inv -> inv.getArgument(0));
        when(conversationRepository.save(any(Conversation.class))).thenReturn(testConversation);

        Flux<String> result = chatService.regenerateMessage(conversationId, assistantMsgId, userId);

        List<String> events = new java.util.ArrayList<>();
        StepVerifier.create(result)
                .recordWith(() -> events)
                .thenConsumeWhile(e -> true)
                .verifyComplete();

        // Verify the old assistant message was deleted
        verify(messageRepository).delete(assistantMsg);

        // Verify we got content and done events
        assertEquals(2, events.size());
        assertTrue(events.get(0).contains("\"type\":\"content\""));
        assertTrue(events.get(0).contains("new response"));
        assertTrue(events.get(1).contains("\"type\":\"done\""));
    }

    @Test
    void regenerateMessage_throwsForNonAssistantMessage() {
        when(conversationRepository.findByIdAndUserId(conversationId, userId))
                .thenReturn(Optional.of(testConversation));

        UUID userMsgId = UUID.randomUUID();
        Message userMsg = new Message();
        userMsg.setId(userMsgId);
        userMsg.setConversation(testConversation);
        userMsg.setRole(MessageRole.USER);
        userMsg.setContent("user message");

        when(messageRepository.findById(userMsgId)).thenReturn(Optional.of(userMsg));

        assertThrows(IllegalArgumentException.class,
                () -> chatService.regenerateMessage(conversationId, userMsgId, userId));
    }

    @Test
    void regenerateMessage_throwsForMessageNotInConversation() {
        when(conversationRepository.findByIdAndUserId(conversationId, userId))
                .thenReturn(Optional.of(testConversation));

        UUID messageId = UUID.randomUUID();
        Conversation otherConversation = new Conversation();
        otherConversation.setId(UUID.randomUUID());

        Message message = new Message();
        message.setId(messageId);
        message.setConversation(otherConversation);
        message.setRole(MessageRole.ASSISTANT);

        when(messageRepository.findById(messageId)).thenReturn(Optional.of(message));

        assertThrows(EntityNotFoundException.class,
                () -> chatService.regenerateMessage(conversationId, messageId, userId));
    }

    @Test
    void regenerateMessage_throwsForNonExistentMessage() {
        when(conversationRepository.findByIdAndUserId(conversationId, userId))
                .thenReturn(Optional.of(testConversation));

        UUID messageId = UUID.randomUUID();
        when(messageRepository.findById(messageId)).thenReturn(Optional.empty());

        assertThrows(EntityNotFoundException.class,
                () -> chatService.regenerateMessage(conversationId, messageId, userId));
    }

    // ── judge pipeline tests (sendMessage) ──────────────────────────────

    @Test
    void sendMessage_judgeEnabled_enhancesResponse_whenScoreBelowThreshold() {
        when(conversationRepository.findByIdAndUserId(conversationId, userId))
                .thenReturn(Optional.of(testConversation));
        when(systemPromptBuilder.build(any(User.class), anyString(), any())).thenReturn("system prompt");
        when(contextWindowService.prepareMessages(any(), anyString(), anyString()))
                .thenReturn(List.of(new OllamaMessage("user", "hello")));
        OllamaChatResponse ollamaResponse = new OllamaChatResponse(
                new OllamaMessage("assistant", "local response"), true, 1000L, 5);
        when(ollamaService.chat(any())).thenReturn(ollamaResponse);
        when(messageRepository.save(any(Message.class))).thenAnswer(inv -> {
            Message m = inv.getArgument(0);
            m.setId(UUID.randomUUID());
            return m;
        });
        when(conversationRepository.save(any(Conversation.class))).thenReturn(testConversation);

        // Enable judge in settings
        when(externalApiSettingsService.getSettings())
                .thenReturn(new ExternalApiSettingsDto(
                        false, "claude-sonnet-4-20250514", false,
                        false, false, 512, 5,
                        false, false,
                        false, false,
                        false, false,
                        FrontierProvider.CLAUDE,
                        true, "judge.gguf", 7.5
                ));
        when(judgeInferenceService.isAvailable()).thenReturn(true);
        when(judgeInferenceService.evaluate(anyString(), anyString()))
                .thenReturn(Optional.of(new JudgeResult(5.0, "Incomplete answer", true)));
        when(frontierApiRouter.isAnyAvailable()).thenReturn(true);
        when(frontierApiRouter.complete(anyString(), anyString()))
                .thenReturn(Optional.of("enhanced cloud response"));

        Message result = chatService.sendMessage(conversationId, userId, "hello");

        assertEquals("enhanced cloud response", result.getContent());
        assertEquals(SourceTag.ENHANCED, result.getSourceTag());
        assertEquals(5.0, result.getJudgeScore());
        assertEquals("Incomplete answer", result.getJudgeReason());
    }

    @Test
    void sendMessage_judgeEnabled_keepsLocal_whenScoreAboveThreshold() {
        when(conversationRepository.findByIdAndUserId(conversationId, userId))
                .thenReturn(Optional.of(testConversation));
        when(systemPromptBuilder.build(any(User.class), anyString(), any())).thenReturn("system prompt");
        when(contextWindowService.prepareMessages(any(), anyString(), anyString()))
                .thenReturn(List.of(new OllamaMessage("user", "hello")));
        OllamaChatResponse ollamaResponse = new OllamaChatResponse(
                new OllamaMessage("assistant", "good local response"), true, 1000L, 5);
        when(ollamaService.chat(any())).thenReturn(ollamaResponse);
        when(messageRepository.save(any(Message.class))).thenAnswer(inv -> {
            Message m = inv.getArgument(0);
            m.setId(UUID.randomUUID());
            return m;
        });
        when(conversationRepository.save(any(Conversation.class))).thenReturn(testConversation);

        when(externalApiSettingsService.getSettings())
                .thenReturn(new ExternalApiSettingsDto(
                        false, "claude-sonnet-4-20250514", false,
                        false, false, 512, 5,
                        false, false,
                        false, false,
                        false, false,
                        FrontierProvider.CLAUDE,
                        true, "judge.gguf", 7.5
                ));
        when(judgeInferenceService.isAvailable()).thenReturn(true);
        when(judgeInferenceService.evaluate(anyString(), anyString()))
                .thenReturn(Optional.of(new JudgeResult(9.0, "Excellent response", false)));

        Message result = chatService.sendMessage(conversationId, userId, "hello");

        assertEquals("good local response", result.getContent());
        assertEquals(SourceTag.LOCAL, result.getSourceTag());
        assertEquals(9.0, result.getJudgeScore());
        verifyNoInteractions(frontierApiRouter);
    }

    @Test
    void sendMessage_judgeDisabled_skipsEvaluation() {
        when(conversationRepository.findByIdAndUserId(conversationId, userId))
                .thenReturn(Optional.of(testConversation));
        when(systemPromptBuilder.build(any(User.class), anyString(), any())).thenReturn("system prompt");
        when(contextWindowService.prepareMessages(any(), anyString(), anyString()))
                .thenReturn(List.of(new OllamaMessage("user", "hello")));
        OllamaChatResponse ollamaResponse = new OllamaChatResponse(
                new OllamaMessage("assistant", "local response"), true, 1000L, 5);
        when(ollamaService.chat(any())).thenReturn(ollamaResponse);
        when(messageRepository.save(any(Message.class))).thenAnswer(inv -> {
            Message m = inv.getArgument(0);
            m.setId(UUID.randomUUID());
            return m;
        });
        when(conversationRepository.save(any(Conversation.class))).thenReturn(testConversation);

        Message result = chatService.sendMessage(conversationId, userId, "hello");

        assertEquals(SourceTag.LOCAL, result.getSourceTag());
        assertNull(result.getJudgeScore());
        verifyNoInteractions(judgeInferenceService);
        verifyNoInteractions(frontierApiRouter);
    }
}
