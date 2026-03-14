package com.myoffgridai.proactive.service;

import com.myoffgridai.common.exception.EntityNotFoundException;
import com.myoffgridai.proactive.model.Insight;
import com.myoffgridai.proactive.model.InsightCategory;
import com.myoffgridai.proactive.repository.InsightRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

/**
 * Service layer for insight CRUD operations and user-facing queries.
 */
@Service
public class InsightService {

    private static final Logger log = LoggerFactory.getLogger(InsightService.class);

    private final InsightRepository insightRepository;

    /**
     * Constructs the insight service.
     *
     * @param insightRepository the insight repository
     */
    public InsightService(InsightRepository insightRepository) {
        this.insightRepository = insightRepository;
    }

    /**
     * Gets active (non-dismissed) insights for a user with pagination.
     *
     * @param userId   the user ID
     * @param pageable the pagination parameters
     * @return paginated insights
     */
    public Page<Insight> getInsights(UUID userId, Pageable pageable) {
        return insightRepository.findByUserIdAndIsDismissedFalseOrderByGeneratedAtDesc(userId, pageable);
    }

    /**
     * Gets active insights filtered by category for a user with pagination.
     *
     * @param userId   the user ID
     * @param category the insight category
     * @param pageable the pagination parameters
     * @return paginated insights
     */
    public Page<Insight> getInsightsByCategory(UUID userId, InsightCategory category, Pageable pageable) {
        return insightRepository.findByUserIdAndCategoryAndIsDismissedFalse(userId, category, pageable);
    }

    /**
     * Gets all unread, non-dismissed insights for a user.
     *
     * @param userId the user ID
     * @return list of unread insights
     */
    public List<Insight> getUnreadInsights(UUID userId) {
        return insightRepository.findByUserIdAndIsReadFalseAndIsDismissedFalse(userId);
    }

    /**
     * Marks an insight as read.
     *
     * @param insightId the insight ID
     * @param userId    the user ID for ownership verification
     * @return the updated insight
     */
    public Insight markRead(UUID insightId, UUID userId) {
        Insight insight = insightRepository.findByIdAndUserId(insightId, userId)
                .orElseThrow(() -> new EntityNotFoundException("Insight not found: " + insightId));
        insight.setIsRead(true);
        insight.setReadAt(Instant.now());
        return insightRepository.save(insight);
    }

    /**
     * Dismisses an insight so it no longer appears in active queries.
     *
     * @param insightId the insight ID
     * @param userId    the user ID for ownership verification
     * @return the updated insight
     */
    public Insight dismiss(UUID insightId, UUID userId) {
        Insight insight = insightRepository.findByIdAndUserId(insightId, userId)
                .orElseThrow(() -> new EntityNotFoundException("Insight not found: " + insightId));
        insight.setIsDismissed(true);
        return insightRepository.save(insight);
    }

    /**
     * Gets the count of unread, non-dismissed insights for a user.
     *
     * @param userId the user ID
     * @return the unread count
     */
    public long getUnreadCount(UUID userId) {
        return insightRepository.countByUserIdAndIsReadFalseAndIsDismissedFalse(userId);
    }

    /**
     * Deletes all insights for a user. Used by privacy wipe.
     *
     * @param userId the user ID
     */
    @Transactional
    public void deleteAllForUser(UUID userId) {
        insightRepository.deleteByUserId(userId);
        log.info("Deleted all insights for user {}", userId);
    }
}
