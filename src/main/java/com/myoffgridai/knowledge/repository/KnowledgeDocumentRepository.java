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

public interface KnowledgeDocumentRepository extends JpaRepository<KnowledgeDocument, UUID> {

    Page<KnowledgeDocument> findByUserIdOrderByUploadedAtDesc(UUID userId, Pageable pageable);

    Optional<KnowledgeDocument> findByIdAndUserId(UUID id, UUID userId);

    List<KnowledgeDocument> findByUserIdAndStatus(UUID userId, DocumentStatus status);

    @Modifying
    void deleteByUserId(UUID userId);

    long countByUserId(UUID userId);
}
