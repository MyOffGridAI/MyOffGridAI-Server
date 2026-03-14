package com.myoffgridai.ai.service;

import com.myoffgridai.ai.dto.OllamaMessage;
import com.myoffgridai.ai.model.Message;
import com.myoffgridai.ai.model.MessageRole;
import com.myoffgridai.ai.repository.MessageRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Pageable;

import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ContextWindowServiceTest {

    @Mock
    private MessageRepository messageRepository;

    @InjectMocks
    private ContextWindowService contextWindowService;

    private UUID conversationId;

    @BeforeEach
    void setUp() {
        conversationId = UUID.randomUUID();
    }

    @Test
    void prepareMessages_systemAlwaysFirst() {
        when(messageRepository.findTopNByConversationIdOrderByCreatedAtDesc(
                any(UUID.class), any(Pageable.class))).thenReturn(List.of());

        List<OllamaMessage> result = contextWindowService.prepareMessages(
                conversationId, "system prompt", "hello");

        assertEquals("system", result.get(0).role());
        assertEquals("system prompt", result.get(0).content());
    }

    @Test
    void prepareMessages_newUserMessageAlwaysLast() {
        when(messageRepository.findTopNByConversationIdOrderByCreatedAtDesc(
                any(UUID.class), any(Pageable.class))).thenReturn(List.of());

        List<OllamaMessage> result = contextWindowService.prepareMessages(
                conversationId, "system prompt", "hello");

        OllamaMessage last = result.get(result.size() - 1);
        assertEquals("user", last.role());
        assertEquals("hello", last.content());
    }

    @Test
    void prepareMessages_includesHistory() {
        Message historyMsg = new Message();
        historyMsg.setRole(MessageRole.ASSISTANT);
        historyMsg.setContent("Previous answer");

        when(messageRepository.findTopNByConversationIdOrderByCreatedAtDesc(
                any(UUID.class), any(Pageable.class))).thenReturn(List.of(historyMsg));

        List<OllamaMessage> result = contextWindowService.prepareMessages(
                conversationId, "system prompt", "new question");

        assertEquals(3, result.size());
        assertEquals("system", result.get(0).role());
        assertEquals("assistant", result.get(1).role());
        assertEquals("user", result.get(2).role());
    }

    @Test
    void prepareMessages_minimumTwoMessages_systemAndUser() {
        when(messageRepository.findTopNByConversationIdOrderByCreatedAtDesc(
                any(UUID.class), any(Pageable.class))).thenReturn(List.of());

        List<OllamaMessage> result = contextWindowService.prepareMessages(
                conversationId, "system prompt", "hello");

        assertTrue(result.size() >= 2);
    }
}
