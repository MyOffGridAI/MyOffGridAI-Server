package com.myoffgridai.ai.repository;

import com.myoffgridai.ai.model.Conversation;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface ConversationRepository extends JpaRepository<Conversation, UUID> {

    Page<Conversation> findByUserIdOrderByUpdatedAtDesc(UUID userId, Pageable pageable);

    Page<Conversation> findByUserIdAndIsArchivedOrderByUpdatedAtDesc(UUID userId, boolean archived, Pageable pageable);

    Optional<Conversation> findByIdAndUserId(UUID id, UUID userId);

    long countByUserId(UUID userId);
}
