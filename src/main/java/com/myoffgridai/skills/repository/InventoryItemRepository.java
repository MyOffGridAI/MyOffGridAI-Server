package com.myoffgridai.skills.repository;

import com.myoffgridai.skills.model.InventoryCategory;
import com.myoffgridai.skills.model.InventoryItem;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface InventoryItemRepository extends JpaRepository<InventoryItem, UUID> {

    List<InventoryItem> findByUserIdOrderByNameAsc(UUID userId);

    List<InventoryItem> findByUserIdAndCategory(UUID userId, InventoryCategory category);

    List<InventoryItem> findByUserIdAndQuantityLessThanEqual(UUID userId, double threshold);

    Optional<InventoryItem> findByIdAndUserId(UUID id, UUID userId);

    @Modifying
    void deleteByUserId(UUID userId);
}
