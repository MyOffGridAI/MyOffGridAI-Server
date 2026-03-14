package com.myoffgridai.skills.dto;

import jakarta.validation.constraints.PositiveOrZero;

/**
 * Request body for updating an existing inventory item.
 *
 * @param quantity          the updated quantity
 * @param notes             the updated notes
 * @param lowStockThreshold the updated low stock threshold
 */
public record UpdateInventoryItemRequest(
        @PositiveOrZero(message = "Quantity must be zero or positive")
        Double quantity,

        String notes,
        Double lowStockThreshold
) {
}
