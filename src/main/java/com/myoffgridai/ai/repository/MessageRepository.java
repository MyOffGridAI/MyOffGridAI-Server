package com.myoffgridai.ai.repository;

import com.myoffgridai.ai.model.Message;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.UUID;

/**
 * Spring Data JPA repository for {@link Message} entities.
 *
 * @author MyOffGridAI
 * @since 0.1.0
 */
public interface MessageRepository extends JpaRepository<Message, UUID> {

    /**
     * Finds all messages in a conversation ordered chronologically.
     *
     * @param conversationId the conversation ID
     * @return messages in ascending creation order
     */
    List<Message> findByConversationIdOrderByCreatedAtAsc(UUID conversationId);

    /**
     * Finds paginated messages in a conversation ordered chronologically.
     *
     * @param conversationId the conversation ID
     * @param pageable       pagination parameters
     * @return a page of messages in ascending creation order
     */
    Page<Message> findByConversationIdOrderByCreatedAtAsc(UUID conversationId, Pageable pageable);

    /**
     * Finds the most recent N messages in a conversation, ordered newest first.
     *
     * @param conversationId the conversation ID
     * @param pageable       pagination parameters controlling the result count
     * @return the most recent messages in descending creation order
     */
    List<Message> findTopNByConversationIdOrderByCreatedAtDesc(UUID conversationId, Pageable pageable);

    /**
     * Counts all messages in a conversation.
     *
     * @param conversationId the conversation ID
     * @return the message count
     */
    long countByConversationId(UUID conversationId);

    /**
     * Deletes all messages belonging to a conversation.
     *
     * @param conversationId the conversation ID
     */
    void deleteByConversationId(UUID conversationId);

    /**
     * Counts all messages across all conversations owned by a user.
     *
     * @param userId the owning user's ID
     * @return the total message count
     */
    @Query("SELECT COUNT(m) FROM Message m WHERE m.conversation.user.id = :userId")
    long countByUserId(@Param("userId") UUID userId);

    /**
     * Deletes all messages across all conversations owned by a user via a bulk JPQL query.
     *
     * @param userId the owning user's ID
     */
    @Modifying
    @Query("DELETE FROM Message m WHERE m.conversation.user.id = :userId")
    void deleteByUserId(@Param("userId") UUID userId);

    /**
     * Deletes all messages in a conversation created after a given message (exclusive).
     *
     * @param conversationId the conversation ID
     * @param messageId      the message whose createdAt serves as the cutoff
     */
    @Modifying
    @Query("DELETE FROM Message m WHERE m.conversation.id = :conversationId "
            + "AND m.createdAt > (SELECT m2.createdAt FROM Message m2 WHERE m2.id = :messageId)")
    void deleteMessagesAfter(@Param("conversationId") UUID conversationId,
                             @Param("messageId") UUID messageId);
}
