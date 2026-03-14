package com.myoffgridai.memory.repository;

import com.myoffgridai.memory.model.VectorDocument;
import com.myoffgridai.memory.model.VectorSourceType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.UUID;

public interface VectorDocumentRepository extends JpaRepository<VectorDocument, UUID> {

    List<VectorDocument> findByUserIdAndSourceType(UUID userId, VectorSourceType sourceType);

    @Modifying
    void deleteBySourceIdAndSourceType(UUID sourceId, VectorSourceType sourceType);

    @Modifying
    void deleteByUserId(UUID userId);

    @Query(value = """
            SELECT * FROM vector_document
            WHERE user_id = :userId
            AND source_type = :sourceType
            ORDER BY embedding <=> CAST(:embedding AS vector)
            LIMIT :topK
            """, nativeQuery = true)
    List<VectorDocument> findMostSimilar(
            @Param("userId") UUID userId,
            @Param("sourceType") String sourceType,
            @Param("embedding") String embedding,
            @Param("topK") int topK
    );

    @Query(value = """
            SELECT * FROM vector_document
            WHERE user_id = :userId
            ORDER BY embedding <=> CAST(:embedding AS vector)
            LIMIT :topK
            """, nativeQuery = true)
    List<VectorDocument> findMostSimilarAcrossTypes(
            @Param("userId") UUID userId,
            @Param("embedding") String embedding,
            @Param("topK") int topK
    );
}
