package com.myoffgridai.privacy.service;

import com.myoffgridai.ai.model.Conversation;
import com.myoffgridai.ai.repository.ConversationRepository;
import com.myoffgridai.ai.repository.MessageRepository;
import com.myoffgridai.memory.repository.MemoryRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * Unit tests for {@link DataExportService}.
 */
@ExtendWith(MockitoExtension.class)
class DataExportServiceTest {

    @Mock
    private ConversationRepository conversationRepository;

    @Mock
    private MessageRepository messageRepository;

    @Mock
    private MemoryRepository memoryRepository;

    private DataExportService dataExportService;

    private UUID userId;

    @BeforeEach
    void setUp() {
        dataExportService = new DataExportService(
                conversationRepository,
                messageRepository,
                memoryRepository
        );
        userId = UUID.randomUUID();
    }

    @Test
    void exportUserData_returnsEncryptedBytes() {
        Conversation conversation = new Conversation();
        conversation.setId(UUID.randomUUID());
        when(conversationRepository.findByUserId(userId)).thenReturn(List.of(conversation));
        when(messageRepository.findByConversationIdOrderByCreatedAtAsc(conversation.getId()))
                .thenReturn(List.of());
        when(memoryRepository.findByUserId(userId)).thenReturn(List.of());

        byte[] result = dataExportService.exportUserData(userId, "test-passphrase");

        assertNotNull(result);
        assertTrue(result.length > 0);
    }

    @Test
    void exportUserData_encryptedSizeGreaterThanZero() {
        Conversation conversation = new Conversation();
        conversation.setId(UUID.randomUUID());
        when(conversationRepository.findByUserId(userId)).thenReturn(List.of(conversation));
        when(messageRepository.findByConversationIdOrderByCreatedAtAsc(conversation.getId()))
                .thenReturn(List.of());
        when(memoryRepository.findByUserId(userId)).thenReturn(List.of());

        byte[] result = dataExportService.exportUserData(userId, "my-secret-key");

        // salt (16) + IV (12) + encrypted data (at least 1 byte + GCM tag 16)
        assertTrue(result.length > 28,
                "Encrypted output should be at least salt + IV + ciphertext; was " + result.length);
    }

    @Test
    void exportUserData_withNoData_stillProducesOutput() {
        when(conversationRepository.findByUserId(userId)).thenReturn(List.of());
        when(memoryRepository.findByUserId(userId)).thenReturn(List.of());

        byte[] result = dataExportService.exportUserData(userId, "empty-data-passphrase");

        assertNotNull(result);
        assertTrue(result.length > 0, "Export should produce output even with no user data");
    }

    @Test
    void exportUserData_differentPassphrases_differentOutput() {
        when(conversationRepository.findByUserId(userId)).thenReturn(List.of());
        when(memoryRepository.findByUserId(userId)).thenReturn(List.of());

        byte[] result1 = dataExportService.exportUserData(userId, "passphrase-alpha");
        byte[] result2 = dataExportService.exportUserData(userId, "passphrase-bravo");

        assertNotNull(result1);
        assertNotNull(result2);
        assertFalse(java.util.Arrays.equals(result1, result2),
                "Different passphrases should produce different encrypted output");
    }
}
