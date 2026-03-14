package com.myoffgridai.knowledge.repository;

import com.myoffgridai.knowledge.model.KnowledgeChunk;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;

import java.util.List;
import java.util.UUID;

public interface KnowledgeChunkRepository extends JpaRepository<KnowledgeChunk, UUID> {

    List<KnowledgeChunk> findByDocumentIdOrderByChunkIndexAsc(UUID documentId);

    @Modifying
    void deleteByDocumentId(UUID documentId);

    @Modifying
    void deleteByUserId(UUID userId);

    long countByDocumentId(UUID documentId);
}
