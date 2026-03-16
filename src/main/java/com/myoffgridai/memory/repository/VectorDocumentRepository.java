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
}
