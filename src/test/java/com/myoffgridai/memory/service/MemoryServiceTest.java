package com.myoffgridai.memory.service;

import com.myoffgridai.common.exception.EntityNotFoundException;
import com.myoffgridai.config.AppConstants;
import com.myoffgridai.memory.model.Memory;
import com.myoffgridai.system.dto.AiSettingsDto;
import com.myoffgridai.system.service.SystemConfigService;
import com.myoffgridai.memory.model.MemoryImportance;
import com.myoffgridai.memory.model.VectorDocument;
import com.myoffgridai.memory.model.VectorSourceType;
import com.myoffgridai.memory.repository.MemoryRepository;
import com.myoffgridai.memory.repository.VectorDocumentRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.access.AccessDeniedException;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@org.mockito.junit.jupiter.MockitoSettings(strictness = org.mockito.quality.Strictness.LENIENT)
class MemoryServiceTest {

    @Mock private MemoryRepository memoryRepository;
    @Mock private VectorDocumentRepository vectorDocumentRepository;
    @Mock private EmbeddingService embeddingService;
    @Mock private SystemConfigService systemConfigService;

    private MemoryService memoryService;
    private UUID userId;

    @BeforeEach
    void setUp() {
        memoryService = new MemoryService(memoryRepository, vectorDocumentRepository,
                embeddingService, systemConfigService);
        userId = UUID.randomUUID();

        // Default AI settings for similarity threshold
        when(systemConfigService.getAiSettings())
                .thenReturn(new AiSettingsDto("test-model", 0.7, 0.45, 5, 2048, 4096, 20));
    }

    @Test
    void createMemory_persistsMemoryAndVectorDocument() {
        float[] embedding = {0.1f, 0.2f};
        when(embeddingService.embed(anyString())).thenReturn(embedding);
        when(memoryRepository.save(any(Memory.class))).thenAnswer(inv -> {
            Memory m = inv.getArgument(0);
            m.setId(UUID.randomUUID());
            return m;
        });
        when(vectorDocumentRepository.save(any(VectorDocument.class))).thenAnswer(inv -> inv.getArgument(0));

        Memory result = memoryService.createMemory(userId, "test fact", MemoryImportance.HIGH, "tag1", null);

        assertNotNull(result);
        assertEquals("test fact", result.getContent());
        assertEquals(MemoryImportance.HIGH, result.getImportance());
        verify(memoryRepository).save(any(Memory.class));
        verify(vectorDocumentRepository).save(any(VectorDocument.class));
    }

    @Test
    void createMemory_savesMemoryEvenIfEmbeddingFails() {
        when(embeddingService.embed(anyString())).thenThrow(new RuntimeException("unavailable"));
        when(memoryRepository.save(any(Memory.class))).thenAnswer(inv -> {
            Memory m = inv.getArgument(0);
            m.setId(UUID.randomUUID());
            return m;
        });

        Memory result = memoryService.createMemory(userId, "test", MemoryImportance.LOW, null, null);
        assertNotNull(result);
        verify(memoryRepository).save(any(Memory.class));
        verify(vectorDocumentRepository, never()).save(any(VectorDocument.class));
    }

    @Test
    void findRelevantMemories_returnsMappedMemories() {
        float[] queryEmb = {0.5f, 0.5f};
        when(embeddingService.embed(anyString())).thenReturn(queryEmb);

        UUID memoryId = UUID.randomUUID();
        VectorDocument doc = new VectorDocument();
        doc.setSourceId(memoryId);
        doc.setEmbedding(new float[]{0.5f, 0.5f});

        when(vectorDocumentRepository.findMostSimilar(eq(userId), eq("MEMORY"), anyString(), anyInt()))
                .thenReturn(List.of(doc));

        Memory memory = new Memory();
        memory.setId(memoryId);
        memory.setUserId(userId);
        memory.setContent("relevant fact");
        memory.setImportance(MemoryImportance.MEDIUM);
        when(memoryRepository.findAllById(anyList())).thenReturn(List.of(memory));

        when(embeddingService.cosineSimilarity(any(), any())).thenReturn(0.9f);
        when(memoryRepository.save(any(Memory.class))).thenAnswer(inv -> inv.getArgument(0));

        List<Memory> results = memoryService.findRelevantMemories(userId, "query", 5);
        assertEquals(1, results.size());
        assertEquals("relevant fact", results.get(0).getContent());
    }

    @Test
    void findRelevantMemories_filtersBelowThreshold() {
        float[] queryEmb = {0.5f, 0.5f};
        when(embeddingService.embed(anyString())).thenReturn(queryEmb);

        UUID memoryId = UUID.randomUUID();
        VectorDocument doc = new VectorDocument();
        doc.setSourceId(memoryId);
        doc.setEmbedding(new float[]{0.1f, 0.9f});

        when(vectorDocumentRepository.findMostSimilar(eq(userId), eq("MEMORY"), anyString(), anyInt()))
                .thenReturn(List.of(doc));

        Memory memory = new Memory();
        memory.setId(memoryId);
        memory.setUserId(userId);
        memory.setContent("irrelevant");
        when(memoryRepository.findAllById(anyList())).thenReturn(List.of(memory));

        // Below threshold
        when(embeddingService.cosineSimilarity(any(), any())).thenReturn(0.3f);

        List<Memory> results = memoryService.findRelevantMemories(userId, "query", 5);
        assertEquals(0, results.size());
    }

    @Test
    void findRelevantMemories_updatesAccessTracking() {
        float[] queryEmb = {0.5f, 0.5f};
        when(embeddingService.embed(anyString())).thenReturn(queryEmb);

        UUID memoryId = UUID.randomUUID();
        VectorDocument doc = new VectorDocument();
        doc.setSourceId(memoryId);
        doc.setEmbedding(new float[]{0.5f, 0.5f});

        when(vectorDocumentRepository.findMostSimilar(eq(userId), eq("MEMORY"), anyString(), anyInt()))
                .thenReturn(List.of(doc));

        Memory memory = new Memory();
        memory.setId(memoryId);
        memory.setUserId(userId);
        memory.setContent("fact");
        memory.setAccessCount(2);
        when(memoryRepository.findAllById(anyList())).thenReturn(List.of(memory));
        when(embeddingService.cosineSimilarity(any(), any())).thenReturn(0.9f);
        when(memoryRepository.save(any(Memory.class))).thenAnswer(inv -> inv.getArgument(0));

        memoryService.findRelevantMemories(userId, "query", 5);

        assertEquals(3, memory.getAccessCount());
        assertNotNull(memory.getLastAccessedAt());
    }

    @Test
    void getMemory_throwsEntityNotFound_whenNotExists() {
        UUID memoryId = UUID.randomUUID();
        when(memoryRepository.findById(memoryId)).thenReturn(Optional.empty());

        assertThrows(EntityNotFoundException.class,
                () -> memoryService.getMemory(memoryId, userId));
    }

    @Test
    void getMemory_throwsAccessDenied_whenNotOwned() {
        UUID memoryId = UUID.randomUUID();
        Memory memory = new Memory();
        memory.setId(memoryId);
        memory.setUserId(UUID.randomUUID()); // different user
        when(memoryRepository.findById(memoryId)).thenReturn(Optional.of(memory));

        assertThrows(AccessDeniedException.class,
                () -> memoryService.getMemory(memoryId, userId));
    }

    @Test
    void deleteMemory_deletesMemoryAndVectorDocument() {
        UUID memoryId = UUID.randomUUID();
        Memory memory = new Memory();
        memory.setId(memoryId);
        memory.setUserId(userId);
        when(memoryRepository.findById(memoryId)).thenReturn(Optional.of(memory));

        memoryService.deleteMemory(memoryId, userId);

        verify(vectorDocumentRepository).deleteBySourceIdAndSourceType(memoryId, VectorSourceType.MEMORY);
        verify(memoryRepository).delete(memory);
    }

    @Test
    void deleteAllMemoriesForUser_deletesBothEntityTypes() {
        memoryService.deleteAllMemoriesForUser(userId);

        verify(vectorDocumentRepository).deleteByUserId(userId);
        verify(memoryRepository).deleteByUserId(userId);
    }

    @Test
    void updateImportance_updatesAndSaves() {
        UUID memoryId = UUID.randomUUID();
        Memory memory = new Memory();
        memory.setId(memoryId);
        memory.setUserId(userId);
        memory.setImportance(MemoryImportance.LOW);
        when(memoryRepository.findById(memoryId)).thenReturn(Optional.of(memory));
        when(memoryRepository.save(any(Memory.class))).thenAnswer(inv -> inv.getArgument(0));

        Memory result = memoryService.updateImportance(memoryId, userId, MemoryImportance.CRITICAL);
        assertEquals(MemoryImportance.CRITICAL, result.getImportance());
    }

    @Test
    void updateTags_updatesAndSaves() {
        UUID memoryId = UUID.randomUUID();
        Memory memory = new Memory();
        memory.setId(memoryId);
        memory.setUserId(userId);
        when(memoryRepository.findById(memoryId)).thenReturn(Optional.of(memory));
        when(memoryRepository.save(any(Memory.class))).thenAnswer(inv -> inv.getArgument(0));

        Memory result = memoryService.updateTags(memoryId, userId, "tag1,tag2");
        assertEquals("tag1,tag2", result.getTags());
    }

    @Test
    void exportMemories_returnsAllUserMemories() {
        Memory m1 = new Memory();
        m1.setUserId(userId);
        Memory m2 = new Memory();
        m2.setUserId(userId);
        when(memoryRepository.findByUserId(userId)).thenReturn(List.of(m1, m2));

        List<Memory> result = memoryService.exportMemories(userId);
        assertEquals(2, result.size());
    }
}
