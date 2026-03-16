package com.myoffgridai.skills.repository;

import com.myoffgridai.skills.model.PlannedTask;
import com.myoffgridai.skills.model.TaskStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;

import java.util.Optional;
import java.util.UUID;

/**
 * Spring Data JPA repository for {@link PlannedTask} entities.
 *
 * @author MyOffGridAI
 * @since 0.1.0
 */
public interface PlannedTaskRepository extends JpaRepository<PlannedTask, UUID> {

    /**
     * Finds all planned tasks for a user with the given status, ordered by creation date descending.
     *
     * @param userId   the owning user's ID
     * @param status   the task status
     * @param pageable pagination parameters
     * @return a page of planned tasks
     */
    Page<PlannedTask> findByUserIdAndStatusOrderByCreatedAtDesc(UUID userId, TaskStatus status, Pageable pageable);

    /**
     * Finds a planned task by its ID and owning user ID.
     *
     * @param id     the task ID
     * @param userId the owning user's ID
     * @return the task if found and owned by the user
     */
    Optional<PlannedTask> findByIdAndUserId(UUID id, UUID userId);

    /**
     * Deletes all planned tasks belonging to a user.
     *
     * @param userId the owning user's ID
     */
    @Modifying
    void deleteByUserId(UUID userId);
}
