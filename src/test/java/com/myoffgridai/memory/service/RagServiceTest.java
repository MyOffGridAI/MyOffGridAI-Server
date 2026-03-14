package com.myoffgridai.memory.service;

import com.myoffgridai.memory.dto.RagContext;
import com.myoffgridai.memory.model.Memory;
import com.myoffgridai.memory.model.MemoryImportance;
import com.myoffgridai.memory.model.VectorDocument;
import com.myoffgridai.memory.model.VectorSourceType;
import com.myoffgridai.memory.repository.VectorDocumentRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@org.mockito.junit.jupiter.MockitoSettings(strictness = org.mockito.quality.Strictness.LENIENT)
class RagServiceTest {

    @Mock private MemoryService memoryService;
    @Mock private VectorDocumentRepository vectorDocumentRepository;
    @Mock private EmbeddingService embeddingService;

    private RagService ragService;
    private UUID userId;

    @BeforeEach
    void setUp() {
        ragService = new RagService(memoryService, vectorDocumentRepository, embeddingService);
        userId = UUID.randomUUID();
    }

    @Test
    void buildRagContext_withMemoriesFound_returnsContext() {
        Memory m1 = new Memory();
        m1.setContent("User has 3 chickens");
        m1.setImportance(MemoryImportance.MEDIUM);
        when(memoryService.findRelevantMemories(eq(userId), anyString(), anyInt()))
                .thenReturn(List.of(m1));
        when(embeddingService.embedAndFormat(anyString())).thenReturn("[0.1,0.2]");
        when(vectorDocumentRepository.findMostSimilar(eq(userId), eq("KNOWLEDGE_CHUNK"), anyString(), anyInt()))
                .thenReturn(List.of());

        RagContext context = ragService.buildRagContext(userId, "how many chickens?");
        assertTrue(context.hasContext());
        assertEquals(1, context.memorySnippets().size());
        assertEquals("User has 3 chickens", context.memorySnippets().get(0));
        assertTrue(context.knowledgeSnippets().isEmpty());
    }

    @Test
    void buildRagContext_withNoMemories_returnsEmptyContext() {
        when(memoryService.findRelevantMemories(eq(userId), anyString(), anyInt()))
                .thenReturn(List.of());
        when(embeddingService.embedAndFormat(anyString())).thenReturn("[0.1,0.2]");
        when(vectorDocumentRepository.findMostSimilar(eq(userId), eq("KNOWLEDGE_CHUNK"), anyString(), anyInt()))
                .thenReturn(List.of());

        RagContext context = ragService.buildRagContext(userId, "test");
        assertFalse(context.hasContext());
        assertTrue(context.memorySnippets().isEmpty());
        assertTrue(context.knowledgeSnippets().isEmpty());
    }

    @Test
    void buildRagContext_withKnowledgeChunks_includesKnowledge() {
        when(memoryService.findRelevantMemories(eq(userId), anyString(), anyInt()))
                .thenReturn(List.of());
        when(embeddingService.embedAndFormat(anyString())).thenReturn("[0.1,0.2]");

        VectorDocument knowledgeDoc = new VectorDocument();
        knowledgeDoc.setContent("Solar panels require 6 hours of sunlight");
        when(vectorDocumentRepository.findMostSimilar(eq(userId), eq("KNOWLEDGE_CHUNK"), anyString(), anyInt()))
                .thenReturn(List.of(knowledgeDoc));

        RagContext context = ragService.buildRagContext(userId, "solar panels");
        assertTrue(context.hasContext());
        assertEquals(1, context.knowledgeSnippets().size());
    }

    @Test
    void buildRagContext_gracefullyHandlesEmbeddingFailure() {
        when(memoryService.findRelevantMemories(eq(userId), anyString(), anyInt()))
                .thenReturn(List.of());
        when(embeddingService.embedAndFormat(anyString()))
                .thenThrow(new RuntimeException("unavailable"));

        RagContext context = ragService.buildRagContext(userId, "test");
        assertFalse(context.hasContext());
    }

    @Test
    void formatContextBlock_withMemories_formatsCorrectly() {
        RagContext context = new RagContext(
                List.of("fact1", "fact2"), List.of(), true, 10);

        String result = ragService.formatContextBlock(context);
        assertTrue(result.contains("[RELEVANT MEMORIES]"));
        assertTrue(result.contains("- fact1"));
        assertTrue(result.contains("- fact2"));
        assertTrue(result.contains("[END MEMORIES]"));
        assertFalse(result.contains("[RELEVANT KNOWLEDGE]"));
    }

    @Test
    void formatContextBlock_withKnowledge_formatsCorrectly() {
        RagContext context = new RagContext(
                List.of(), List.of("chunk1"), true, 5);

        String result = ragService.formatContextBlock(context);
        assertTrue(result.contains("[RELEVANT KNOWLEDGE]"));
        assertTrue(result.contains("- chunk1"));
        assertTrue(result.contains("[END KNOWLEDGE]"));
    }

    @Test
    void formatContextBlock_withBoth_formatsBothSections() {
        RagContext context = new RagContext(
                List.of("memory"), List.of("knowledge"), true, 10);

        String result = ragService.formatContextBlock(context);
        assertTrue(result.contains("[RELEVANT MEMORIES]"));
        assertTrue(result.contains("[RELEVANT KNOWLEDGE]"));
    }

    @Test
    void formatContextBlock_noContext_returnsEmpty() {
        RagContext context = new RagContext(List.of(), List.of(), false, 0);
        String result = ragService.formatContextBlock(context);
        assertEquals("", result);
    }

    @Test
    void formatContextBlock_null_returnsEmpty() {
        String result = ragService.formatContextBlock(null);
        assertEquals("", result);
    }
}
