package com.myoffgridai.ai.service;

import com.myoffgridai.ai.model.Conversation;
import com.myoffgridai.ai.model.Message;
import com.myoffgridai.ai.model.MessageRole;
import com.myoffgridai.ai.repository.MessageRepository;
import com.myoffgridai.auth.model.User;
import com.myoffgridai.common.exception.EntityNotFoundException;
import com.myoffgridai.knowledge.dto.KnowledgeDocumentDto;
import com.myoffgridai.knowledge.model.DocumentStatus;
import com.myoffgridai.knowledge.model.KnowledgeDocument;
import com.myoffgridai.knowledge.repository.KnowledgeDocumentRepository;
import com.myoffgridai.knowledge.service.FileStorageService;
import com.myoffgridai.knowledge.service.KnowledgeService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * Unit tests for {@link ChatExportService}.
 *
 * <p>Verifies PDF generation, Knowledge Library save, ownership enforcement,
 * empty conversation handling, and default title fallback.</p>
 */
@ExtendWith(MockitoExtension.class)
class ChatExportServiceTest {

    @Mock private ChatService chatService;
    @Mock private MessageRepository messageRepository;
    @Mock private FileStorageService fileStorageService;
    @Mock private KnowledgeDocumentRepository knowledgeDocumentRepository;
    @Mock private KnowledgeService knowledgeService;

    @InjectMocks private ChatExportService chatExportService;

    private UUID userId;
    private UUID conversationId;
    private User testUser;
    private Conversation testConversation;

    @BeforeEach
    void setUp() {
        userId = UUID.randomUUID();
        conversationId = UUID.randomUUID();

        testUser = new User();
        testUser.setId(userId);
        testUser.setUsername("testuser");

        testConversation = new Conversation();
        testConversation.setId(conversationId);
        testConversation.setUser(testUser);
        testConversation.setTitle("Test Conversation");
        testConversation.setCreatedAt(Instant.now());
        testConversation.setUpdatedAt(Instant.now());
    }

    @Test
    void generateConversationPdf_success_returnsPdfBytes() {
        Message userMsg = createMessage(MessageRole.USER, "Hello there");
        Message assistantMsg = createMessage(MessageRole.ASSISTANT, "Hi! How can I help?");

        when(chatService.getConversation(conversationId, userId)).thenReturn(testConversation);
        when(messageRepository.findByConversationIdOrderByCreatedAtAsc(conversationId))
                .thenReturn(List.of(userMsg, assistantMsg));

        byte[] pdf = chatExportService.generateConversationPdf(conversationId, userId);

        assertNotNull(pdf);
        assertTrue(pdf.length > 0);
        // PDF magic bytes: %PDF
        String header = new String(pdf, 0, 4);
        assertEquals("%PDF", header);
    }

    @Test
    void generateConversationPdf_emptyConversation_returnsPdfWithTitleOnly() {
        when(chatService.getConversation(conversationId, userId)).thenReturn(testConversation);
        when(messageRepository.findByConversationIdOrderByCreatedAtAsc(conversationId))
                .thenReturn(List.of());

        byte[] pdf = chatExportService.generateConversationPdf(conversationId, userId);

        assertNotNull(pdf);
        assertTrue(pdf.length > 0);
        String header = new String(pdf, 0, 4);
        assertEquals("%PDF", header);
    }

    @Test
    void generateConversationPdf_notOwner_throwsEntityNotFound() {
        when(chatService.getConversation(conversationId, userId))
                .thenThrow(new EntityNotFoundException("Conversation not found"));

        assertThrows(EntityNotFoundException.class,
                () -> chatExportService.generateConversationPdf(conversationId, userId));
    }

    @Test
    void saveConversationToLibrary_success_createsDocumentAndTriggersProcessing() {
        Message userMsg = createMessage(MessageRole.USER, "Test message");

        when(chatService.getConversation(conversationId, userId)).thenReturn(testConversation);
        when(messageRepository.findByConversationIdOrderByCreatedAtAsc(conversationId))
                .thenReturn(List.of(userMsg));
        when(fileStorageService.storeBytes(eq(userId), any(byte[].class), anyString()))
                .thenReturn("/storage/test.pdf");

        KnowledgeDocument savedDoc = new KnowledgeDocument();
        savedDoc.setId(UUID.randomUUID());
        savedDoc.setUserId(userId);
        savedDoc.setFilename("Test_Conversation.pdf");
        savedDoc.setDisplayName("Test Conversation");
        savedDoc.setMimeType("application/pdf");
        savedDoc.setStoragePath("/storage/test.pdf");
        savedDoc.setFileSizeBytes(1000);
        savedDoc.setStatus(DocumentStatus.PENDING);

        when(knowledgeDocumentRepository.save(any(KnowledgeDocument.class))).thenReturn(savedDoc);

        KnowledgeDocumentDto expectedDto = new KnowledgeDocumentDto(
                savedDoc.getId(), "Test_Conversation.pdf", "Test Conversation",
                "application/pdf", 1000, DocumentStatus.PENDING, null, 0,
                null, null, false, false);
        when(knowledgeService.toDto(savedDoc)).thenReturn(expectedDto);

        KnowledgeDocumentDto result = chatExportService.saveConversationToLibrary(conversationId, userId);

        assertNotNull(result);
        assertEquals("Test Conversation", result.displayName());
        assertEquals("application/pdf", result.mimeType());
        assertEquals(DocumentStatus.PENDING, result.status());

        // Verify entity fields were set correctly
        ArgumentCaptor<KnowledgeDocument> docCaptor = ArgumentCaptor.forClass(KnowledgeDocument.class);
        verify(knowledgeDocumentRepository).save(docCaptor.capture());
        KnowledgeDocument captured = docCaptor.getValue();
        assertEquals(userId, captured.getUserId());
        assertEquals("Test Conversation", captured.getDisplayName());
        assertEquals("application/pdf", captured.getMimeType());
        assertEquals(DocumentStatus.PENDING, captured.getStatus());

        // Verify async processing was triggered
        verify(knowledgeService).processDocumentAsync(savedDoc.getId());
    }

    @Test
    void saveConversationToLibrary_nullTitle_usesDefaultDisplayName() {
        testConversation.setTitle(null);
        Message userMsg = createMessage(MessageRole.USER, "Test");

        when(chatService.getConversation(conversationId, userId)).thenReturn(testConversation);
        when(messageRepository.findByConversationIdOrderByCreatedAtAsc(conversationId))
                .thenReturn(List.of(userMsg));
        when(fileStorageService.storeBytes(eq(userId), any(byte[].class), anyString()))
                .thenReturn("/storage/export.pdf");

        KnowledgeDocument savedDoc = new KnowledgeDocument();
        savedDoc.setId(UUID.randomUUID());
        savedDoc.setUserId(userId);
        savedDoc.setStatus(DocumentStatus.PENDING);

        when(knowledgeDocumentRepository.save(any(KnowledgeDocument.class))).thenReturn(savedDoc);

        KnowledgeDocumentDto expectedDto = new KnowledgeDocumentDto(
                savedDoc.getId(), "Chat_Export.pdf", "Chat Export",
                "application/pdf", 500, DocumentStatus.PENDING, null, 0,
                null, null, false, false);
        when(knowledgeService.toDto(savedDoc)).thenReturn(expectedDto);

        KnowledgeDocumentDto result = chatExportService.saveConversationToLibrary(conversationId, userId);

        assertNotNull(result);
        assertEquals("Chat Export", result.displayName());

        // Verify the default title was used
        ArgumentCaptor<KnowledgeDocument> docCaptor = ArgumentCaptor.forClass(KnowledgeDocument.class);
        verify(knowledgeDocumentRepository).save(docCaptor.capture());
        assertEquals("Chat Export", docCaptor.getValue().getDisplayName());
    }

    private Message createMessage(MessageRole role, String content) {
        Message msg = new Message();
        msg.setId(UUID.randomUUID());
        msg.setRole(role);
        msg.setContent(content);
        msg.setHasRagContext(false);
        msg.setCreatedAt(Instant.now());
        return msg;
    }
}
