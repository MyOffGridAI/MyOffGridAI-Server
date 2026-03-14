package com.myoffgridai.skills.repository;

import com.myoffgridai.skills.model.ExecutionStatus;
import com.myoffgridai.skills.model.SkillExecution;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface SkillExecutionRepository extends JpaRepository<SkillExecution, UUID> {

    Page<SkillExecution> findByUserIdOrderByStartedAtDesc(UUID userId, Pageable pageable);

    Page<SkillExecution> findBySkillIdAndUserIdOrderByStartedAtDesc(UUID skillId, UUID userId, Pageable pageable);

    List<SkillExecution> findByUserIdAndStatus(UUID userId, ExecutionStatus status);
}
