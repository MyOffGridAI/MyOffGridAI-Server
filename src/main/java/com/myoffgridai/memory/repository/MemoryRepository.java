package com.myoffgridai.memory.repository;

import com.myoffgridai.memory.model.Memory;
import com.myoffgridai.memory.model.MemoryImportance;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;

import java.util.List;
import java.util.UUID;

public interface MemoryRepository extends JpaRepository<Memory, UUID> {

    Page<Memory> findByUserIdOrderByCreatedAtDesc(UUID userId, Pageable pageable);

    Page<Memory> findByUserIdAndImportance(UUID userId, MemoryImportance importance, Pageable pageable);

    Page<Memory> findByUserIdAndTagsContaining(UUID userId, String tag, Pageable pageable);

    List<Memory> findByUserId(UUID userId);

    @Modifying
    void deleteByUserId(UUID userId);

    long countByUserId(UUID userId);
}
