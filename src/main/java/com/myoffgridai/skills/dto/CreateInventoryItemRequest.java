package com.myoffgridai.skills.dto;

import com.myoffgridai.skills.model.InventoryCategory;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;

/**
 * Request body for creating a new inventory item.
 *
 * @param name              the item name
 * @param category          the item category
 * @param quantity          the initial quantity
 * @param unit              the unit of measurement
 * @param notes             optional notes
 * @param lowStockThreshold optional low stock threshold
 */
public record CreateInventoryItemRequest(
        @NotBlank(message = "Name is required")
        String name,

        @NotNull(message = "Category is required")
        InventoryCategory category,

        @PositiveOrZero(message = "Quantity must be zero or positive")
        double quantity,

        String unit,
        String notes,
        Double lowStockThreshold
) {
}
