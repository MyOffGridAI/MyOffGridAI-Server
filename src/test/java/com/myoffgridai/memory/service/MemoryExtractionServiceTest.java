package com.myoffgridai.memory.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.myoffgridai.ai.dto.OllamaChatResponse;
import com.myoffgridai.ai.dto.OllamaMessage;
import com.myoffgridai.ai.service.OllamaService;
import com.myoffgridai.memory.model.MemoryImportance;
import com.myoffgridai.system.dto.AiSettingsDto;
import com.myoffgridai.system.service.SystemConfigService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class MemoryExtractionServiceTest {

    @Mock private OllamaService ollamaService;
    @Mock private MemoryService memoryService;
    @Mock private SystemConfigService systemConfigService;

    private MemoryExtractionService extractionService;
    private UUID userId;
    private UUID conversationId;

    @BeforeEach
    void setUp() {
        extractionService = new MemoryExtractionService(
                ollamaService, memoryService, new ObjectMapper(), systemConfigService);
        userId = UUID.randomUUID();
        conversationId = UUID.randomUUID();

        lenient().when(systemConfigService.getAiSettings())
                .thenReturn(new AiSettingsDto("test-model", 0.7, 0.45, 5, 2048, 4096, 20));
    }

    @Test
    void extractAndStore_validJsonResponse_createsMemories() {
        String jsonResponse = """
                [{"content":"User has solar panels","importance":"HIGH","tags":"energy,solar"}]""";
        OllamaChatResponse response = new OllamaChatResponse(
                new OllamaMessage("assistant", jsonResponse), true, 100L, 10);
        when(ollamaService.chat(any())).thenReturn(response);

        extractionService.extractAndStore(userId, conversationId, "I have solar panels", "That's great!");

        verify(memoryService).createMemory(
                eq(userId), eq("User has solar panels"),
                eq(MemoryImportance.HIGH), eq("energy,solar"), eq(conversationId));
    }

    @Test
    void extractAndStore_multipleFactsResponse_createsMultipleMemories() {
        String jsonResponse = """
                [
                    {"content":"User lives off-grid","importance":"MEDIUM","tags":"lifestyle"},
                    {"content":"User has 5 acres","importance":"HIGH","tags":"property"}
                ]""";
        OllamaChatResponse response = new OllamaChatResponse(
                new OllamaMessage("assistant", jsonResponse), true, 100L, 20);
        when(ollamaService.chat(any())).thenReturn(response);

        extractionService.extractAndStore(userId, conversationId, "msg", "resp");

        verify(memoryService, times(2)).createMemory(any(), anyString(), any(), any(), any());
    }

    @Test
    void extractAndStore_invalidJsonResponse_doesNotThrow() {
        OllamaChatResponse response = new OllamaChatResponse(
                new OllamaMessage("assistant", "not json at all"), true, 100L, 5);
        when(ollamaService.chat(any())).thenReturn(response);

        assertDoesNotThrow(() ->
                extractionService.extractAndStore(userId, conversationId, "msg", "resp"));
        verify(memoryService, never()).createMemory(any(), anyString(), any(), any(), any());
    }

    @Test
    void extractAndStore_emptyJsonArray_createsNoMemories() {
        OllamaChatResponse response = new OllamaChatResponse(
                new OllamaMessage("assistant", "[]"), true, 100L, 2);
        when(ollamaService.chat(any())).thenReturn(response);

        extractionService.extractAndStore(userId, conversationId, "msg", "resp");

        verify(memoryService, never()).createMemory(any(), anyString(), any(), any(), any());
    }

    @Test
    void extractAndStore_ollamaError_doesNotThrow() {
        when(ollamaService.chat(any())).thenThrow(new RuntimeException("timeout"));

        assertDoesNotThrow(() ->
                extractionService.extractAndStore(userId, conversationId, "msg", "resp"));
    }

    @Test
    void extractAndStore_invalidImportance_defaultsToMedium() {
        String jsonResponse = """
                [{"content":"fact","importance":"INVALID","tags":""}]""";
        OllamaChatResponse response = new OllamaChatResponse(
                new OllamaMessage("assistant", jsonResponse), true, 100L, 5);
        when(ollamaService.chat(any())).thenReturn(response);

        extractionService.extractAndStore(userId, conversationId, "msg", "resp");

        verify(memoryService).createMemory(
                eq(userId), eq("fact"), eq(MemoryImportance.MEDIUM), eq(""), eq(conversationId));
    }

    @Test
    void extractAndStore_markdownCodeFence_parsesCorrectly() {
        String jsonResponse = """
                ```json
                [{"content":"User grows tomatoes","importance":"LOW","tags":"garden"}]
                ```""";
        OllamaChatResponse response = new OllamaChatResponse(
                new OllamaMessage("assistant", jsonResponse), true, 100L, 10);
        when(ollamaService.chat(any())).thenReturn(response);

        extractionService.extractAndStore(userId, conversationId, "msg", "resp");

        verify(memoryService).createMemory(
                eq(userId), eq("User grows tomatoes"),
                eq(MemoryImportance.LOW), eq("garden"), eq(conversationId));
    }
}
