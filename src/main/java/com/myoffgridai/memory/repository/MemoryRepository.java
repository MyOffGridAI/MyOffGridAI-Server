package com.myoffgridai.memory.repository;

import com.myoffgridai.memory.model.Memory;
import com.myoffgridai.memory.model.MemoryImportance;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;

import java.util.List;
import java.util.UUID;

/**
 * Spring Data JPA repository for {@link Memory} entities.
 *
 * @author MyOffGridAI
 * @since 0.1.0
 */
public interface MemoryRepository extends JpaRepository<Memory, UUID> {

    /**
     * Returns paginated memories for a user, ordered by most recently created.
     *
     * @param userId   the owning user's ID
     * @param pageable pagination parameters
     * @return a page of memories
     */
    Page<Memory> findByUserIdOrderByCreatedAtDesc(UUID userId, Pageable pageable);

    /**
     * Returns paginated memories for a user filtered by importance level.
     *
     * @param userId     the owning user's ID
     * @param importance the importance filter
     * @param pageable   pagination parameters
     * @return a page of memories matching the importance level
     */
    Page<Memory> findByUserIdAndImportance(UUID userId, MemoryImportance importance, Pageable pageable);

    /**
     * Returns paginated memories for a user whose tags contain the specified value.
     *
     * @param userId   the owning user's ID
     * @param tag      the tag substring to match
     * @param pageable pagination parameters
     * @return a page of memories whose tags contain the given tag
     */
    Page<Memory> findByUserIdAndTagsContaining(UUID userId, String tag, Pageable pageable);

    /**
     * Finds all memories belonging to a user without pagination.
     *
     * @param userId the owning user's ID
     * @return all memories for the user
     */
    List<Memory> findByUserId(UUID userId);

    /**
     * Deletes all memories belonging to a user.
     *
     * @param userId the owning user's ID
     */
    @Modifying
    void deleteByUserId(UUID userId);

    /**
     * Counts all memories belonging to a user.
     *
     * @param userId the owning user's ID
     * @return the memory count
     */
    long countByUserId(UUID userId);

    /**
     * Fetches memories by a list of IDs that are owned by a specific user.
     *
     * @param ids    the memory IDs to look up
     * @param userId the owning user's ID
     * @return memories matching the IDs and owned by the user
     */
    List<Memory> findByIdInAndUserId(List<UUID> ids, UUID userId);

    /**
     * Returns paginated memories that are either owned by the user or shared by any user.
     *
     * @param userId   the requesting user's ID
     * @param pageable pagination parameters
     * @return a page of owned and shared memories
     */
    Page<Memory> findByUserIdOrSharedTrueOrderByCreatedAtDesc(UUID userId, Pageable pageable);
}
