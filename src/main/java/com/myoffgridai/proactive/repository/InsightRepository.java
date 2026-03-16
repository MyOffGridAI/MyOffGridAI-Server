package com.myoffgridai.proactive.repository;

import com.myoffgridai.proactive.model.Insight;
import com.myoffgridai.proactive.model.InsightCategory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Spring Data JPA repository for {@link Insight} entities.
 *
 * @author MyOffGridAI
 * @since 0.1.0
 */
@Repository
public interface InsightRepository extends JpaRepository<Insight, UUID> {

    /**
     * Finds active (non-dismissed) insights for a user, newest first.
     *
     * @param userId   the user ID
     * @param pageable the pagination parameters
     * @return paginated active insights
     */
    Page<Insight> findByUserIdAndIsDismissedFalseOrderByGeneratedAtDesc(UUID userId, Pageable pageable);

    /**
     * Finds active insights filtered by category for a user.
     *
     * @param userId   the user ID
     * @param category the insight category filter
     * @param pageable the pagination parameters
     * @return paginated insights matching the category
     */
    Page<Insight> findByUserIdAndCategoryAndIsDismissedFalse(UUID userId, InsightCategory category, Pageable pageable);

    /**
     * Finds all unread, non-dismissed insights for a user.
     *
     * @param userId the user ID
     * @return list of unread active insights
     */
    List<Insight> findByUserIdAndIsReadFalseAndIsDismissedFalse(UUID userId);

    /**
     * Counts unread, non-dismissed insights for a user.
     *
     * @param userId the user ID
     * @return the unread count
     */
    long countByUserIdAndIsReadFalseAndIsDismissedFalse(UUID userId);

    /**
     * Finds an insight by ID scoped to a specific user.
     *
     * @param id     the insight ID
     * @param userId the user ID
     * @return the insight, or empty if not found
     */
    Optional<Insight> findByIdAndUserId(UUID id, UUID userId);

    /**
     * Counts all insights for a user.
     *
     * @param userId the user ID
     * @return the insight count
     */
    long countByUserId(UUID userId);

    /**
     * Deletes all insights for a user.
     *
     * @param userId the user ID
     */
    @Modifying
    void deleteByUserId(UUID userId);
}
