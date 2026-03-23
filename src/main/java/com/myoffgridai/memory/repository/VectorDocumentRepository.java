package com.myoffgridai.memory.repository;

import com.myoffgridai.memory.model.VectorDocument;
import com.myoffgridai.memory.model.VectorSourceType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.UUID;

/**
 * Spring Data JPA repository for {@link VectorDocument} entities with pgvector similarity search.
 *
 * @author MyOffGridAI
 * @since 0.1.0
 */
public interface VectorDocumentRepository extends JpaRepository<VectorDocument, UUID> {

    /**
     * Finds all vector documents for a user filtered by source type.
     *
     * @param userId     the owning user's ID
     * @param sourceType the vector source type filter
     * @return vector documents matching the user and source type
     */
    List<VectorDocument> findByUserIdAndSourceType(UUID userId, VectorSourceType sourceType);

    /**
     * Deletes all vector documents matching a specific source entity and type.
     *
     * @param sourceId   the source entity ID
     * @param sourceType the vector source type
     */
    @Modifying
    void deleteBySourceIdAndSourceType(UUID sourceId, VectorSourceType sourceType);

    /**
     * Deletes all vector documents matching any of the given source IDs and source type.
     *
     * @param sourceIds  the list of source entity IDs
     * @param sourceType the vector source type
     */
    @Modifying
    void deleteBySourceIdInAndSourceType(List<UUID> sourceIds, VectorSourceType sourceType);

    /**
     * Deletes all vector documents belonging to a user.
     *
     * @param userId the owning user's ID
     */
    @Modifying
    void deleteByUserId(UUID userId);

    /**
     * Finds the most similar vector documents for a user and source type using pgvector cosine distance.
     *
     * @param userId     the owning user's ID
     * @param sourceType the source type to filter by
     * @param embedding  the query embedding in pgvector string format
     * @param topK       the maximum number of results to return
     * @return the most similar documents ordered by ascending cosine distance
     */
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

    /**
     * Finds the most similar vector documents for a user across all source types using pgvector cosine distance.
     *
     * @param userId    the owning user's ID
     * @param embedding the query embedding in pgvector string format
     * @param topK      the maximum number of results to return
     * @return the most similar documents ordered by ascending cosine distance
     */
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

    /**
     * Finds the most similar MEMORY vector documents owned by the user OR linked to shared memories.
     *
     * @param userId     the requesting user's ID
     * @param sourceType the source type to filter by (MEMORY)
     * @param embedding  the query embedding in pgvector string format
     * @param topK       the maximum number of results to return
     * @return the most similar documents ordered by ascending cosine distance
     */
    @Query(value = """
            SELECT * FROM vector_document
            WHERE (user_id = :userId OR source_id IN (
                SELECT m.id FROM memories m WHERE m.shared = true
            ))
            AND source_type = :sourceType
            ORDER BY embedding <=> CAST(:embedding AS vector)
            LIMIT :topK
            """, nativeQuery = true)
    List<VectorDocument> findMostSimilarIncludingSharedMemories(
            @Param("userId") UUID userId,
            @Param("sourceType") String sourceType,
            @Param("embedding") String embedding,
            @Param("topK") int topK
    );

    /**
     * Finds the most similar vector documents including shared knowledge documents.
     * Returns vectors owned by the user OR vectors whose source chunk belongs to a shared document.
     *
     * @param userId     the requesting user's ID
     * @param sourceType the source type to filter by
     * @param embedding  the query embedding in pgvector string format
     * @param topK       the maximum number of results to return
     * @return the most similar documents ordered by ascending cosine distance
     */
    @Query(value = """
            SELECT * FROM vector_document
            WHERE (user_id = :userId OR source_id IN (
                SELECT kc.id FROM knowledge_chunks kc
                JOIN knowledge_documents kd ON kc.document_id = kd.id
                WHERE kd.is_shared = true
            ))
            AND source_type = :sourceType
            ORDER BY embedding <=> CAST(:embedding AS vector)
            LIMIT :topK
            """, nativeQuery = true)
    List<VectorDocument> findMostSimilarIncludingShared(
            @Param("userId") UUID userId,
            @Param("sourceType") String sourceType,
            @Param("embedding") String embedding,
            @Param("topK") int topK
    );
}
