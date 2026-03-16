package com.myoffgridai.skills.repository;

import com.myoffgridai.skills.model.InventoryCategory;
import com.myoffgridai.skills.model.InventoryItem;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Spring Data JPA repository for {@link InventoryItem} entities.
 *
 * @author MyOffGridAI
 * @since 0.1.0
 */
public interface InventoryItemRepository extends JpaRepository<InventoryItem, UUID> {

    /**
     * Finds all inventory items for a user, ordered by name ascending.
     *
     * @param userId the owning user's ID
     * @return list of inventory items sorted alphabetically
     */
    List<InventoryItem> findByUserIdOrderByNameAsc(UUID userId);

    /**
     * Finds all inventory items for a user in the given category.
     *
     * @param userId   the owning user's ID
     * @param category the inventory category
     * @return list of matching inventory items
     */
    List<InventoryItem> findByUserIdAndCategory(UUID userId, InventoryCategory category);

    /**
     * Finds all inventory items for a user with quantity at or below the given threshold.
     *
     * @param userId    the owning user's ID
     * @param threshold the quantity threshold
     * @return list of low-stock inventory items
     */
    List<InventoryItem> findByUserIdAndQuantityLessThanEqual(UUID userId, double threshold);

    /**
     * Finds an inventory item by its ID and owning user ID.
     *
     * @param id     the item ID
     * @param userId the owning user's ID
     * @return the item if found and owned by the user
     */
    Optional<InventoryItem> findByIdAndUserId(UUID id, UUID userId);

    /**
     * Deletes all inventory items belonging to a user.
     *
     * @param userId the owning user's ID
     */
    @Modifying
    void deleteByUserId(UUID userId);
}
