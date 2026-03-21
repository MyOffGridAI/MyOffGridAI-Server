package com.myoffgridai.knowledge.repository;

import com.myoffgridai.knowledge.model.DocumentStatus;
import com.myoffgridai.knowledge.model.KnowledgeDocument;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Spring Data JPA repository for {@link KnowledgeDocument} entities.
 *
 * @author MyOffGridAI
 * @since 0.1.0
 */
public interface KnowledgeDocumentRepository extends JpaRepository<KnowledgeDocument, UUID> {

    /**
     * Finds all documents for a user, ordered by upload date descending.
     *
     * @param userId   the owning user's ID
     * @param pageable pagination parameters
     * @return a page of knowledge documents
     */
    Page<KnowledgeDocument> findByUserIdOrderByUploadedAtDesc(UUID userId, Pageable pageable);

    /**
     * Finds a document by its ID and owning user ID.
     *
     * @param id     the document ID
     * @param userId the owning user's ID
     * @return the document if found and owned by the user
     */
    Optional<KnowledgeDocument> findByIdAndUserId(UUID id, UUID userId);

    /**
     * Finds all documents for a user with the given processing status.
     *
     * @param userId the owning user's ID
     * @param status the document processing status
     * @return list of matching documents
     */
    List<KnowledgeDocument> findByUserIdAndStatus(UUID userId, DocumentStatus status);

    /**
     * Deletes all documents belonging to a user.
     *
     * @param userId the owning user's ID
     */
    @Modifying
    void deleteByUserId(UUID userId);

    /**
     * Counts the total number of documents belonging to a user.
     *
     * @param userId the owning user's ID
     * @return the document count
     */
    long countByUserId(UUID userId);

    /**
     * Finds all shared documents from other users, ordered by upload date descending.
     *
     * @param userId   the requesting user's ID (excluded from results)
     * @param pageable pagination parameters
     * @return a page of shared documents not owned by the requesting user
     */
    Page<KnowledgeDocument> findByIsSharedTrueAndUserIdNotOrderByUploadedAtDesc(UUID userId, Pageable pageable);

    /**
     * Finds a shared document by its ID, regardless of owner.
     *
     * @param id the document ID
     * @return the document if it exists and is shared
     */
    Optional<KnowledgeDocument> findByIdAndIsSharedTrue(UUID id);
}
