package com.myoffgridai.skills.repository;

import com.myoffgridai.skills.model.PlannedTask;
import com.myoffgridai.skills.model.TaskStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;

import java.util.Optional;
import java.util.UUID;

public interface PlannedTaskRepository extends JpaRepository<PlannedTask, UUID> {

    Page<PlannedTask> findByUserIdAndStatusOrderByCreatedAtDesc(UUID userId, TaskStatus status, Pageable pageable);

    Optional<PlannedTask> findByIdAndUserId(UUID id, UUID userId);

    @Modifying
    void deleteByUserId(UUID userId);
}
