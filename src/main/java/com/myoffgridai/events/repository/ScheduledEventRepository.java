package com.myoffgridai.events.repository;

import com.myoffgridai.events.model.EventType;
import com.myoffgridai.events.model.ScheduledEvent;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Spring Data JPA repository for {@link ScheduledEvent} entities.
 */
@Repository
public interface ScheduledEventRepository extends JpaRepository<ScheduledEvent, UUID> {

    /**
     * Finds all events for a user with pagination.
     */
    Page<ScheduledEvent> findAllByUserId(UUID userId, Pageable pageable);

    /**
     * Finds a single event by ID scoped to a specific user.
     */
    Optional<ScheduledEvent> findByIdAndUserId(UUID id, UUID userId);

    /**
     * Finds all enabled events of a given type (for scheduler use).
     */
    List<ScheduledEvent> findByIsEnabledTrueAndEventType(EventType eventType);

    /**
     * Lists all events for a user ordered by creation date descending.
     */
    List<ScheduledEvent> findAllByUserIdOrderByCreatedAtDesc(UUID userId);

    /**
     * Deletes all events for a user (used during user deletion).
     */
    void deleteByUserId(UUID userId);

    /**
     * Counts events for a user.
     */
    long countByUserId(UUID userId);
}
