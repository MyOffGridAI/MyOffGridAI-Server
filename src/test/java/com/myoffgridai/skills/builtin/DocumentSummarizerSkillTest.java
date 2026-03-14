package com.myoffgridai.skills.builtin;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.myoffgridai.ai.dto.OllamaChatResponse;
import com.myoffgridai.ai.dto.OllamaMessage;
import com.myoffgridai.ai.service.OllamaService;
import com.myoffgridai.config.AppConstants;
import com.myoffgridai.knowledge.dto.KnowledgeDocumentDto;
import com.myoffgridai.knowledge.model.DocumentStatus;
import com.myoffgridai.knowledge.model.KnowledgeChunk;
import com.myoffgridai.knowledge.service.KnowledgeService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class DocumentSummarizerSkillTest {

    @Mock private KnowledgeService knowledgeService;
    @Mock private OllamaService ollamaService;

    private DocumentSummarizerSkill skill;
    private UUID userId;

    @BeforeEach
    void setUp() {
        skill = new DocumentSummarizerSkill(knowledgeService, ollamaService, new ObjectMapper());
        userId = UUID.randomUUID();
    }

    @Test
    void getSkillName_returnsDocumentSummarizer() {
        assertEquals(AppConstants.SKILL_DOCUMENT_SUMMARIZER, skill.getSkillName());
    }

    @Test
    void execute_missingDocumentId_throwsIllegalArgument() {
        assertThrows(IllegalArgumentException.class,
                () -> skill.execute(userId, Map.of()));
    }

    @Test
    void execute_blankDocumentId_throwsIllegalArgument() {
        assertThrows(IllegalArgumentException.class,
                () -> skill.execute(userId, Map.of("documentId", "  ")));
    }

    @Test
    void execute_documentNotReady_returnsError() {
        UUID docId = UUID.randomUUID();
        KnowledgeDocumentDto doc = new KnowledgeDocumentDto(
                docId, "test.txt", "Test Doc", "text/plain",
                1000L, DocumentStatus.PENDING, null, 0, Instant.now(), Instant.now());
        when(knowledgeService.getDocument(docId, userId)).thenReturn(doc);

        Map<String, Object> result = skill.execute(userId,
                Map.of("documentId", docId.toString()));

        assertTrue(result.containsKey("error"));
        assertTrue(result.get("error").toString().contains("not ready"));
    }

    @Test
    void execute_readyDocument_returnsSummary() {
        UUID docId = UUID.randomUUID();
        KnowledgeDocumentDto doc = new KnowledgeDocumentDto(
                docId, "guide.pdf", "Solar Guide", "application/pdf",
                5000L, DocumentStatus.READY, null, 3, Instant.now(), Instant.now());
        when(knowledgeService.getDocument(docId, userId)).thenReturn(doc);

        KnowledgeChunk chunk = new KnowledgeChunk();
        chunk.setContent("Solar panels need direct sunlight for optimal efficiency.");
        when(knowledgeService.getChunks(docId, userId)).thenReturn(List.of(chunk));

        String jsonResponse = """
                {"summary":"This guide covers solar panels.","keyPoints":["Point 1"],"actionItems":["Check panels"]}""";
        when(ollamaService.chat(any())).thenReturn(
                new OllamaChatResponse(new OllamaMessage("assistant", jsonResponse), true, 100L, 50));

        Map<String, Object> result = skill.execute(userId,
                Map.of("documentId", docId.toString()));

        assertEquals("Solar Guide", result.get("documentName"));
        assertEquals("This guide covers solar panels.", result.get("summary"));
        assertNotNull(result.get("keyPoints"));
        assertNotNull(result.get("actionItems"));
        assertEquals(1, result.get("chunksAnalyzed"));
    }

    @Test
    void execute_invalidJsonResponse_returnsRawText() {
        UUID docId = UUID.randomUUID();
        KnowledgeDocumentDto doc = new KnowledgeDocumentDto(
                docId, "test.txt", null, "text/plain",
                1000L, DocumentStatus.READY, null, 1, Instant.now(), Instant.now());
        when(knowledgeService.getDocument(docId, userId)).thenReturn(doc);

        KnowledgeChunk chunk = new KnowledgeChunk();
        chunk.setContent("Some content here.");
        when(knowledgeService.getChunks(docId, userId)).thenReturn(List.of(chunk));

        when(ollamaService.chat(any())).thenReturn(
                new OllamaChatResponse(new OllamaMessage("assistant", "not json"), true, 100L, 50));

        Map<String, Object> result = skill.execute(userId,
                Map.of("documentId", docId.toString()));

        assertEquals("test.txt", result.get("documentName"));
        assertEquals("not json", result.get("summary"));
        assertTrue(((List<?>) result.get("keyPoints")).isEmpty());
        assertTrue(((List<?>) result.get("actionItems")).isEmpty());
    }

    @Test
    void execute_usesDisplayNameOverFilename() {
        UUID docId = UUID.randomUUID();
        KnowledgeDocumentDto doc = new KnowledgeDocumentDto(
                docId, "file.txt", "My Custom Name", "text/plain",
                1000L, DocumentStatus.READY, null, 1, Instant.now(), Instant.now());
        when(knowledgeService.getDocument(docId, userId)).thenReturn(doc);
        when(knowledgeService.getChunks(docId, userId)).thenReturn(List.of());

        String jsonResponse = """
                {"summary":"Summary","keyPoints":[],"actionItems":[]}""";
        when(ollamaService.chat(any())).thenReturn(
                new OllamaChatResponse(new OllamaMessage("assistant", jsonResponse), true, 100L, 50));

        Map<String, Object> result = skill.execute(userId,
                Map.of("documentId", docId.toString()));

        assertEquals("My Custom Name", result.get("documentName"));
    }
}
