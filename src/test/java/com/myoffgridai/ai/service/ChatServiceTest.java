package com.myoffgridai.ai.service;

import com.myoffgridai.ai.dto.OllamaChatChunk;
import com.myoffgridai.ai.dto.OllamaChatResponse;
import com.myoffgridai.ai.dto.OllamaMessage;
import com.myoffgridai.ai.model.Conversation;
import com.myoffgridai.ai.model.Message;
import com.myoffgridai.ai.model.MessageRole;
import com.myoffgridai.ai.repository.ConversationRepository;
import com.myoffgridai.ai.repository.MessageRepository;
import com.myoffgridai.auth.model.Role;
import com.myoffgridai.auth.model.User;
import com.myoffgridai.auth.repository.UserRepository;
import com.myoffgridai.common.exception.EntityNotFoundException;
import com.myoffgridai.memory.service.MemoryExtractionService;
import com.myoffgridai.memory.service.RagService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import reactor.core.publisher.Flux;
import reactor.test.StepVerifier;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ChatServiceTest {

    @Mock private ConversationRepository conversationRepository;
    @Mock private MessageRepository messageRepository;
    @Mock private UserRepository userRepository;
    @Mock private OllamaService ollamaService;
    @Mock private SystemPromptBuilder systemPromptBuilder;
    @Mock private ContextWindowService contextWindowService;
    @Mock private RagService ragService;
    @Mock private MemoryExtractionService memoryExtractionService;

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

        OllamaChatChunk chunk1 = new OllamaChatChunk(new OllamaMessage("assistant", "Hi"), false);
        OllamaChatChunk chunk2 = new OllamaChatChunk(new OllamaMessage("assistant", " there"), true);
        when(ollamaService.chatStream(any())).thenReturn(Flux.just(chunk1, chunk2));
        when(messageRepository.save(any(Message.class))).thenAnswer(inv -> inv.getArgument(0));
        when(conversationRepository.save(any(Conversation.class))).thenReturn(testConversation);

        Flux<String> result = chatService.streamMessage(conversationId, userId, "hello");

        StepVerifier.create(result)
                .expectNext("Hi")
                .expectNext(" there")
                .verifyComplete();
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
}
