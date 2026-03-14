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

public interface MessageRepository extends JpaRepository<Message, UUID> {

    List<Message> findByConversationIdOrderByCreatedAtAsc(UUID conversationId);

    Page<Message> findByConversationIdOrderByCreatedAtAsc(UUID conversationId, Pageable pageable);

    List<Message> findTopNByConversationIdOrderByCreatedAtDesc(UUID conversationId, Pageable pageable);

    long countByConversationId(UUID conversationId);

    void deleteByConversationId(UUID conversationId);

    @Query("SELECT COUNT(m) FROM Message m WHERE m.conversation.user.id = :userId")
    long countByUserId(@Param("userId") UUID userId);

    @Modifying
    @Query("DELETE FROM Message m WHERE m.conversation.user.id = :userId")
    void deleteByUserId(@Param("userId") UUID userId);
}
