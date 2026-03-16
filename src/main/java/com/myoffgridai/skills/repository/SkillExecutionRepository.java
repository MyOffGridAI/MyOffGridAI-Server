package com.myoffgridai.skills.repository;

import com.myoffgridai.skills.model.ExecutionStatus;
import com.myoffgridai.skills.model.SkillExecution;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

/**
 * Spring Data JPA repository for {@link SkillExecution} entities.
 *
 * @author MyOffGridAI
 * @since 0.1.0
 */
public interface SkillExecutionRepository extends JpaRepository<SkillExecution, UUID> {

    /**
     * Finds all executions for a user, ordered by start time descending.
     *
     * @param userId   the user's ID
     * @param pageable pagination parameters
     * @return a page of skill executions
     */
    Page<SkillExecution> findByUserIdOrderByStartedAtDesc(UUID userId, Pageable pageable);

    /**
     * Finds all executions of a specific skill for a user, ordered by start time descending.
     *
     * @param skillId  the skill's ID
     * @param userId   the user's ID
     * @param pageable pagination parameters
     * @return a page of skill executions
     */
    Page<SkillExecution> findBySkillIdAndUserIdOrderByStartedAtDesc(UUID skillId, UUID userId, Pageable pageable);

    /**
     * Finds all executions for a user with the given status.
     *
     * @param userId the user's ID
     * @param status the execution status
     * @return list of matching executions
     */
    List<SkillExecution> findByUserIdAndStatus(UUID userId, ExecutionStatus status);
}
