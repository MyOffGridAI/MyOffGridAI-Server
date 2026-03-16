package com.myoffgridai.knowledge.repository;

import com.myoffgridai.knowledge.model.KnowledgeChunk;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;

import java.util.List;
import java.util.UUID;

/**
 * Spring Data JPA repository for {@link KnowledgeChunk} entities.
 *
 * @author MyOffGridAI
 * @since 0.1.0
 */
public interface KnowledgeChunkRepository extends JpaRepository<KnowledgeChunk, UUID> {

    /**
     * Finds all chunks for a document, ordered by chunk index ascending.
     *
     * @param documentId the parent document's ID
     * @return list of chunks in order
     */
    List<KnowledgeChunk> findByDocumentIdOrderByChunkIndexAsc(UUID documentId);

    /**
     * Deletes all chunks belonging to a document.
     *
     * @param documentId the parent document's ID
     */
    @Modifying
    void deleteByDocumentId(UUID documentId);

    /**
     * Deletes all chunks belonging to a user.
     *
     * @param userId the owning user's ID
     */
    @Modifying
    void deleteByUserId(UUID userId);

    /**
     * Counts the total number of chunks for a document.
     *
     * @param documentId the parent document's ID
     * @return the chunk count
     */
    long countByDocumentId(UUID documentId);
}
