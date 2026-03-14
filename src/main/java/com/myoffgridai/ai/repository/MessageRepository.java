package com.myoffgridai.ai.repository;

import com.myoffgridai.ai.model.Message;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface MessageRepository extends JpaRepository<Message, UUID> {

    List<Message> findByConversationIdOrderByCreatedAtAsc(UUID conversationId);

    Page<Message> findByConversationIdOrderByCreatedAtAsc(UUID conversationId, Pageable pageable);

    List<Message> findTopNByConversationIdOrderByCreatedAtDesc(UUID conversationId, Pageable pageable);

    long countByConversationId(UUID conversationId);

    void deleteByConversationId(UUID conversationId);
}
