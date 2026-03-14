package com.myoffgridai.proactive.repository;

import com.myoffgridai.proactive.model.Insight;
import com.myoffgridai.proactive.model.InsightCategory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface InsightRepository extends JpaRepository<Insight, UUID> {

    Page<Insight> findByUserIdAndIsDismissedFalseOrderByGeneratedAtDesc(UUID userId, Pageable pageable);

    Page<Insight> findByUserIdAndCategoryAndIsDismissedFalse(UUID userId, InsightCategory category, Pageable pageable);

    List<Insight> findByUserIdAndIsReadFalseAndIsDismissedFalse(UUID userId);

    long countByUserIdAndIsReadFalseAndIsDismissedFalse(UUID userId);

    Optional<Insight> findByIdAndUserId(UUID id, UUID userId);

    @Modifying
    void deleteByUserId(UUID userId);
}
