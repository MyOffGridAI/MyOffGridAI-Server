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

public interface ConversationRepository extends JpaRepository<Conversation, UUID> {

    Page<Conversation> findByUserIdOrderByUpdatedAtDesc(UUID userId, Pageable pageable);

    Page<Conversation> findByUserIdAndIsArchivedOrderByUpdatedAtDesc(UUID userId, boolean archived, Pageable pageable);

    Optional<Conversation> findByIdAndUserId(UUID id, UUID userId);

    long countByUserId(UUID userId);

    List<Conversation> findByUserId(UUID userId);

    @Modifying
    @Query("DELETE FROM Conversation c WHERE c.user.id = :userId")
    void deleteByUserId(@Param("userId") UUID userId);
}
