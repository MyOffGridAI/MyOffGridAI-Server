package com.myoffgridai.skills.dto;

import com.myoffgridai.skills.model.InventoryCategory;
import com.myoffgridai.skills.model.InventoryItem;

import java.time.Instant;
import java.util.UUID;

/**
 * Data transfer object for inventory items.
 *
 * @param id                the item ID
 * @param name              the item name
 * @param category          the item category
 * @param quantity          the item quantity
 * @param unit              the unit of measurement
 * @param notes             additional notes
 * @param lowStockThreshold the low stock alert threshold
 * @param createdAt         the creation timestamp
 * @param updatedAt         the last update timestamp
 */
public record InventoryItemDto(
        UUID id,
        String name,
        InventoryCategory category,
        double quantity,
        String unit,
        String notes,
        Double lowStockThreshold,
        Instant createdAt,
        Instant updatedAt
) {

    /**
     * Converts an {@link InventoryItem} entity to a DTO.
     *
     * @param item the inventory item entity
     * @return the inventory item DTO
     */
    public static InventoryItemDto from(InventoryItem item) {
        return new InventoryItemDto(
                item.getId(),
                item.getName(),
                item.getCategory(),
                item.getQuantity(),
                item.getUnit(),
                item.getNotes(),
                item.getLowStockThreshold(),
                item.getCreatedAt(),
                item.getUpdatedAt()
        );
    }
}
