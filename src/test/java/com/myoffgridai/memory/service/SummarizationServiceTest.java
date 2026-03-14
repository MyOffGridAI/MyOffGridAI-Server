package com.myoffgridai.memory.service;

import com.myoffgridai.ai.dto.OllamaChatResponse;
import com.myoffgridai.ai.dto.OllamaMessage;
import com.myoffgridai.ai.model.Conversation;
import com.myoffgridai.ai.model.Message;
import com.myoffgridai.ai.model.MessageRole;
import com.myoffgridai.ai.repository.ConversationRepository;
import com.myoffgridai.ai.repository.MessageRepository;
import com.myoffgridai.ai.service.OllamaService;
import com.myoffgridai.auth.model.Role;
import com.myoffgridai.auth.model.User;
import com.myoffgridai.config.AppConstants;
import com.myoffgridai.memory.model.Memory;
import com.myoffgridai.memory.model.MemoryImportance;
import com.myoffgridai.memory.repository.MemoryRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@org.mockito.junit.jupiter.MockitoSettings(strictness = org.mockito.quality.Strictness.LENIENT)
class SummarizationServiceTest {

    @Mock private ConversationRepository conversationRepository;
    @Mock private MessageRepository messageRepository;
    @Mock private OllamaService ollamaService;
    @Mock private MemoryService memoryService;
    @Mock private MemoryRepository memoryRepository;

    private SummarizationService summarizationService;
    private UUID userId;
    private UUID conversationId;

    @BeforeEach
    void setUp() {
        summarizationService = new SummarizationService(
                conversationRepository, messageRepository, ollamaService,
                memoryService, memoryRepository);
        userId = UUID.randomUUID();
        conversationId = UUID.randomUUID();
    }

    @Test
    void summarizeConversation_createsCriticalMemoryWithCorrectTag() {
        Message m1 = new Message();
        m1.setRole(MessageRole.USER);
        m1.setContent("Hello");
        Message m2 = new Message();
        m2.setRole(MessageRole.ASSISTANT);
        m2.setContent("Hi there!");

        when(messageRepository.findByConversationIdOrderByCreatedAtAsc(conversationId))
                .thenReturn(List.of(m1, m2));

        OllamaChatResponse response = new OllamaChatResponse(
                new OllamaMessage("assistant", "Summary of conversation"), true, 100L, 10);
        when(ollamaService.chat(any())).thenReturn(response);

        Memory expectedMemory = new Memory();
        expectedMemory.setContent("Summary of conversation");
        when(memoryService.createMemory(
                eq(userId), eq("Summary of conversation"),
                eq(MemoryImportance.CRITICAL),
                eq(AppConstants.MEMORY_SUMMARIZATION_TAG),
                eq(conversationId)))
                .thenReturn(expectedMemory);

        Memory result = summarizationService.summarizeConversation(conversationId, userId);

        assertNotNull(result);
        assertEquals("Summary of conversation", result.getContent());
        verify(memoryService).createMemory(
                eq(userId), anyString(), eq(MemoryImportance.CRITICAL),
                eq(AppConstants.MEMORY_SUMMARIZATION_TAG), eq(conversationId));
    }

    @Test
    void scheduledNightlySummarization_skipsAlreadySummarized() {
        User user = new User();
        user.setId(userId);
        user.setUsername("test");
        user.setRole(Role.ROLE_MEMBER);

        Conversation conv = new Conversation();
        conv.setId(conversationId);
        conv.setUser(user);
        conv.setMessageCount(20);
        conv.setCreatedAt(Instant.now().minus(30, ChronoUnit.DAYS));

        // Already has a summary memory
        Memory existingSummary = new Memory();
        existingSummary.setSourceConversationId(conversationId);
        existingSummary.setTags(AppConstants.MEMORY_SUMMARIZATION_TAG);

        when(conversationRepository.findAll(any(Pageable.class)))
                .thenReturn(new PageImpl<>(List.of(conv)));
        when(memoryRepository.findByUserId(userId)).thenReturn(List.of(existingSummary));

        summarizationService.scheduledNightlySummarization();

        // Should not summarize because it already has a summary
        verify(memoryService, never()).createMemory(any(), anyString(), any(), any(), any());
    }

    @Test
    void scheduledNightlySummarization_skipsRecentConversations() {
        User user = new User();
        user.setId(userId);
        user.setUsername("test");
        user.setRole(Role.ROLE_MEMBER);

        Conversation conv = new Conversation();
        conv.setId(conversationId);
        conv.setUser(user);
        conv.setMessageCount(20);
        conv.setCreatedAt(Instant.now().minus(1, ChronoUnit.DAYS)); // Too recent

        when(conversationRepository.findAll(any(Pageable.class)))
                .thenReturn(new PageImpl<>(List.of(conv)));

        summarizationService.scheduledNightlySummarization();

        verify(memoryService, never()).createMemory(any(), anyString(), any(), any(), any());
    }

    @Test
    void scheduledNightlySummarization_skipsLowMessageCount() {
        User user = new User();
        user.setId(userId);
        user.setUsername("test");
        user.setRole(Role.ROLE_MEMBER);

        Conversation conv = new Conversation();
        conv.setId(conversationId);
        conv.setUser(user);
        conv.setMessageCount(3); // Below threshold
        conv.setCreatedAt(Instant.now().minus(30, ChronoUnit.DAYS));

        when(conversationRepository.findAll(any(Pageable.class)))
                .thenReturn(new PageImpl<>(List.of(conv)));

        summarizationService.scheduledNightlySummarization();

        verify(memoryService, never()).createMemory(any(), anyString(), any(), any(), any());
    }
}
