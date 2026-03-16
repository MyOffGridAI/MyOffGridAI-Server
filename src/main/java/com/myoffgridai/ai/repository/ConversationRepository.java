package com.myoffgridai.ai.repository;

import com.myoffgridai.ai.model.Conversation;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Spring Data JPA repository for {@link Conversation} entities.
 *
 * @author MyOffGridAI
 * @since 0.1.0
 */
public interface ConversationRepository extends JpaRepository<Conversation, UUID> {

    /**
     * Returns paginated conversations for a user, ordered by most recently updated.
     *
     * @param userId   the owning user's ID
     * @param pageable pagination parameters
     * @return a page of conversations
     */
    Page<Conversation> findByUserIdOrderByUpdatedAtDesc(UUID userId, Pageable pageable);

    /**
     * Returns paginated conversations for a user filtered by archive status.
     *
     * @param userId   the owning user's ID
     * @param archived whether to return archived or non-archived conversations
     * @param pageable pagination parameters
     * @return a page of conversations matching the archive filter
     */
    Page<Conversation> findByUserIdAndIsArchivedOrderByUpdatedAtDesc(UUID userId, boolean archived, Pageable pageable);

    /**
     * Finds a conversation by its ID and owning user ID to enforce ownership.
     *
     * @param id     the conversation ID
     * @param userId the owning user's ID
     * @return the conversation if found and owned by the user
     */
    Optional<Conversation> findByIdAndUserId(UUID id, UUID userId);

    /**
     * Counts all conversations belonging to a user.
     *
     * @param userId the owning user's ID
     * @return the conversation count
     */
    long countByUserId(UUID userId);

    /**
     * Finds all conversations belonging to a user without pagination.
     *
     * @param userId the owning user's ID
     * @return all conversations for the user
     */
    List<Conversation> findByUserId(UUID userId);

    /**
     * Searches conversations by title using case-insensitive containment matching.
     *
     * @param userId   the owning user's ID
     * @param title    the search term to match within conversation titles
     * @param pageable pagination parameters
     * @return a page of conversations whose titles contain the search term
     */
    Page<Conversation> findByUserIdAndTitleContainingIgnoreCaseOrderByUpdatedAtDesc(
            UUID userId, String title, Pageable pageable);

    /**
     * Deletes all conversations belonging to a user via a bulk JPQL query.
     *
     * @param userId the owning user's ID
     */
    @Modifying
    @Query("DELETE FROM Conversation c WHERE c.user.id = :userId")
    void deleteByUserId(@Param("userId") UUID userId);
}
