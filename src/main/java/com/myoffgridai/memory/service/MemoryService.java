package com.myoffgridai.memory.service;

import com.myoffgridai.common.exception.EntityNotFoundException;
import com.myoffgridai.config.AppConstants;
import com.myoffgridai.memory.dto.MemoryDto;
import com.myoffgridai.system.service.SystemConfigService;
import com.myoffgridai.memory.dto.MemorySearchResultDto;
import com.myoffgridai.memory.model.Memory;
import com.myoffgridai.memory.model.MemoryImportance;
import com.myoffgridai.memory.model.VectorDocument;
import com.myoffgridai.memory.model.VectorSourceType;
import com.myoffgridai.memory.repository.MemoryRepository;
import com.myoffgridai.memory.repository.VectorDocumentRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Service for managing user memories and their associated vector embeddings.
 *
 * <p>Every method that operates on a specific user's data enforces ownership
 * via {@link #assertOwnership(UUID, UUID)} to guarantee cross-user isolation.</p>
 */
@Service
public class MemoryService {

    private static final Logger log = LoggerFactory.getLogger(MemoryService.class);

    private final MemoryRepository memoryRepository;
    private final VectorDocumentRepository vectorDocumentRepository;
    private final EmbeddingService embeddingService;
    private final SystemConfigService systemConfigService;

    /**
     * Constructs the memory service.
     *
     * @param memoryRepository         the memory data access layer
     * @param vectorDocumentRepository the vector document data access layer
     * @param embeddingService         the embedding generation service
     * @param systemConfigService      the system config service for dynamic AI settings
     */
    public MemoryService(MemoryRepository memoryRepository,
                          VectorDocumentRepository vectorDocumentRepository,
                          EmbeddingService embeddingService,
                          SystemConfigService systemConfigService) {
        this.memoryRepository = memoryRepository;
        this.vectorDocumentRepository = vectorDocumentRepository;
        this.embeddingService = embeddingService;
        this.systemConfigService = systemConfigService;
    }

    /**
     * Creates a new memory with an associated vector embedding.
     *
     * @param userId               the owning user's ID
     * @param content              the memory text
     * @param importance           the importance level
     * @param tags                 comma-separated tags (nullable)
     * @param sourceConversationId the source conversation ID (nullable)
     * @return the persisted Memory entity
     */
    @Transactional
    public Memory createMemory(UUID userId, String content, MemoryImportance importance,
                                String tags, UUID sourceConversationId) {
        log.info("Creating memory for user: {}", userId);

        Memory memory = new Memory();
        memory.setUserId(userId);
        memory.setContent(content);
        memory.setImportance(importance);
        memory.setTags(tags);
        memory.setSourceConversationId(sourceConversationId);
        Memory saved = memoryRepository.save(memory);

        try {
            float[] embedding = embeddingService.embed(content);
            VectorDocument vectorDoc = new VectorDocument();
            vectorDoc.setUserId(userId);
            vectorDoc.setContent(content);
            vectorDoc.setEmbedding(embedding);
            vectorDoc.setSourceType(VectorSourceType.MEMORY);
            vectorDoc.setSourceId(saved.getId());
            vectorDocumentRepository.save(vectorDoc);
            log.debug("Created vector document for memory: {}", saved.getId());
        } catch (Exception e) {
            log.warn("Failed to create embedding for memory {}: {}. "
                    + "Memory saved but not searchable.", saved.getId(), e.getMessage());
        }

        return saved;
    }

    /**
     * Finds memories relevant to the query text using vector similarity search.
     *
     * @param userId    the user's ID
     * @param queryText the text to search for
     * @param topK      maximum number of results
     * @return list of relevant memories ordered by similarity (most similar first)
     */
    @Transactional
    public List<Memory> findRelevantMemories(UUID userId, String queryText, int topK) {
        log.debug("Finding relevant memories for user: {}", userId);

        List<MemoryWithScore> scored = findRelevantMemoriesInternal(userId, queryText, topK);
        return scored.stream().map(MemoryWithScore::memory).toList();
    }

    /**
     * Searches memories with similarity scores for API responses.
     *
     * @param userId    the user's ID
     * @param queryText the text to search for
     * @param topK      maximum number of results
     * @return list of memory search results with similarity scores
     */
    @Transactional
    public List<MemorySearchResultDto> searchMemoriesWithScores(UUID userId, String queryText, int topK) {
        log.debug("Searching memories with scores for user: {}", userId);

        List<MemoryWithScore> scored = findRelevantMemoriesInternal(userId, queryText, topK);
        return scored.stream()
                .map(mws -> new MemorySearchResultDto(toDto(mws.memory()), mws.score()))
                .toList();
    }

    /**
     * Fetches a memory by ID, enforcing user ownership.
     *
     * @param memoryId the memory ID
     * @param userId   the caller's user ID
     * @return the Memory entity
     * @throws EntityNotFoundException if not found
     * @throws AccessDeniedException   if not owned by caller
     */
    public Memory getMemory(UUID memoryId, UUID userId) {
        Memory memory = memoryRepository.findById(memoryId)
                .orElseThrow(() -> new EntityNotFoundException("Memory not found: " + memoryId));
        assertOwnership(memory.getUserId(), userId);
        return memory;
    }

    /**
     * Updates the importance of a memory.
     *
     * @param memoryId   the memory ID
     * @param userId     the caller's user ID
     * @param importance the new importance level
     * @return the updated Memory
     */
    @Transactional
    public Memory updateImportance(UUID memoryId, UUID userId, MemoryImportance importance) {
        log.info("Updating importance for memory: {} to {}", memoryId, importance);
        Memory memory = getMemory(memoryId, userId);
        memory.setImportance(importance);
        return memoryRepository.save(memory);
    }

    /**
     * Updates the tags of a memory.
     *
     * @param memoryId the memory ID
     * @param userId   the caller's user ID
     * @param tags     the new tags string
     * @return the updated Memory
     */
    @Transactional
    public Memory updateTags(UUID memoryId, UUID userId, String tags) {
        log.info("Updating tags for memory: {}", memoryId);
        Memory memory = getMemory(memoryId, userId);
        memory.setTags(tags);
        return memoryRepository.save(memory);
    }

    /**
     * Deletes a memory and its associated vector document.
     *
     * @param memoryId the memory ID
     * @param userId   the caller's user ID
     */
    @Transactional
    public void deleteMemory(UUID memoryId, UUID userId) {
        log.info("Deleting memory: {} for user: {}", memoryId, userId);
        Memory memory = getMemory(memoryId, userId);
        vectorDocumentRepository.deleteBySourceIdAndSourceType(memoryId, VectorSourceType.MEMORY);
        memoryRepository.delete(memory);
    }

    /**
     * Deletes all memories and vector documents for a user (privacy wipe).
     *
     * @param userId the user's ID
     */
    @Transactional
    public void deleteAllMemoriesForUser(UUID userId) {
        log.info("Deleting all memories for user: {}", userId);
        vectorDocumentRepository.deleteByUserId(userId);
        memoryRepository.deleteByUserId(userId);
    }

    /**
     * Exports all memories for a user (data export).
     *
     * @param userId the user's ID
     * @return all memories belonging to the user
     */
    public List<Memory> exportMemories(UUID userId) {
        log.info("Exporting all memories for user: {}", userId);
        return memoryRepository.findByUserId(userId);
    }

    /**
     * Returns paginated memories for a user with optional filters.
     *
     * @param userId     the user's ID
     * @param importance optional importance filter
     * @param tag        optional tag filter
     * @param pageable   pagination parameters
     * @return paginated memories
     */
    public Page<Memory> getMemories(UUID userId, MemoryImportance importance, String tag, Pageable pageable) {
        if (importance != null) {
            return memoryRepository.findByUserIdAndImportance(userId, importance, pageable);
        }
        if (tag != null && !tag.isBlank()) {
            return memoryRepository.findByUserIdAndTagsContaining(userId, tag, pageable);
        }
        return memoryRepository.findByUserIdOrderByCreatedAtDesc(userId, pageable);
    }

    /**
     * Converts a Memory entity to a MemoryDto.
     *
     * @param memory the memory entity
     * @return the DTO
     */
    public MemoryDto toDto(Memory memory) {
        return new MemoryDto(
                memory.getId(), memory.getContent(), memory.getImportance(),
                memory.getTags(), memory.getSourceConversationId(),
                memory.getCreatedAt(), memory.getUpdatedAt(),
                memory.getLastAccessedAt(), memory.getAccessCount());
    }

    private List<MemoryWithScore> findRelevantMemoriesInternal(UUID userId, String queryText, int topK) {
        float[] queryEmbedding;
        try {
            queryEmbedding = embeddingService.embed(queryText);
        } catch (Exception e) {
            log.warn("Failed to embed query for memory search: {}", e.getMessage());
            return List.of();
        }

        String formattedEmbedding = EmbeddingService.formatEmbedding(queryEmbedding);
        List<VectorDocument> docs = vectorDocumentRepository.findMostSimilar(
                userId, VectorSourceType.MEMORY.name(), formattedEmbedding, topK);

        if (docs.isEmpty()) {
            return List.of();
        }

        List<UUID> memoryIds = docs.stream()
                .map(VectorDocument::getSourceId)
                .filter(Objects::nonNull)
                .toList();
        Map<UUID, Memory> memoryMap = memoryRepository.findAllById(memoryIds)
                .stream().collect(Collectors.toMap(Memory::getId, m -> m));

        List<MemoryWithScore> results = new ArrayList<>();
        for (VectorDocument doc : docs) {
            Memory memory = memoryMap.get(doc.getSourceId());
            if (memory != null) {
                float similarity = embeddingService.cosineSimilarity(queryEmbedding, doc.getEmbedding());
                double similarityThreshold = systemConfigService.getAiSettings().similarityThreshold();
                if (similarity >= similarityThreshold) {
                    results.add(new MemoryWithScore(memory, similarity));
                }
            }
        }

        // Update access tracking
        for (MemoryWithScore mws : results) {
            mws.memory().setLastAccessedAt(Instant.now());
            mws.memory().setAccessCount(mws.memory().getAccessCount() + 1);
            memoryRepository.save(mws.memory());
        }

        results.sort(Comparator.comparing(MemoryWithScore::score).reversed());
        return results;
    }

    private void assertOwnership(UUID resourceUserId, UUID callerId) {
        if (!resourceUserId.equals(callerId)) {
            throw new AccessDeniedException("Memory access denied");
        }
    }

    private record MemoryWithScore(Memory memory, float score) {}
}
