package com.myoffgridai.skills.builtin;

import com.myoffgridai.common.exception.EntityNotFoundException;
import com.myoffgridai.config.AppConstants;
import com.myoffgridai.skills.model.InventoryCategory;
import com.myoffgridai.skills.model.InventoryItem;
import com.myoffgridai.skills.repository.InventoryItemRepository;
import com.myoffgridai.skills.service.BuiltInSkill;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.util.*;

/**
 * Built-in skill for managing homestead inventory (food, tools, supplies, etc.).
 *
 * <p>Supports list, add, update, delete, and low-stock actions.</p>
 */
@Component
public class InventoryTrackerSkill implements BuiltInSkill {

    private static final Logger log = LoggerFactory.getLogger(InventoryTrackerSkill.class);

    private final InventoryItemRepository inventoryItemRepository;

    /**
     * Constructs the inventory tracker skill.
     *
     * @param inventoryItemRepository the inventory item repository
     */
    public InventoryTrackerSkill(InventoryItemRepository inventoryItemRepository) {
        this.inventoryItemRepository = inventoryItemRepository;
    }

    @Override
    public String getSkillName() {
        return AppConstants.SKILL_INVENTORY_TRACKER;
    }

    /**
     * Executes an inventory action.
     *
     * @param userId the user's ID
     * @param params must contain "action" key; other keys depend on the action
     * @return map with action, items/item, and message
     */
    @Override
    public Map<String, Object> execute(UUID userId, Map<String, Object> params) {
        String action = (String) params.getOrDefault("action", "list");
        log.info("Inventory action '{}' for user: {}", action, userId);

        return switch (action) {
            case "add" -> addItem(userId, params);
            case "update" -> updateItem(userId, params);
            case "delete" -> deleteItem(userId, params);
            case "low-stock" -> lowStock(userId);
            default -> listItems(userId, params);
        };
    }

    private Map<String, Object> listItems(UUID userId, Map<String, Object> params) {
        String categoryStr = (String) params.get("category");
        List<InventoryItem> items;

        if (categoryStr != null && !categoryStr.isBlank()) {
            InventoryCategory category = InventoryCategory.valueOf(categoryStr.toUpperCase());
            items = inventoryItemRepository.findByUserIdAndCategory(userId, category);
        } else {
            items = inventoryItemRepository.findByUserIdOrderByNameAsc(userId);
        }

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("action", "list");
        result.put("items", items.stream().map(this::toMap).toList());
        result.put("message", "Found " + items.size() + " inventory items");
        return result;
    }

    private Map<String, Object> addItem(UUID userId, Map<String, Object> params) {
        InventoryItem item = new InventoryItem();
        item.setUserId(userId);
        item.setName((String) params.get("name"));
        item.setCategory(InventoryCategory.valueOf(
                ((String) params.getOrDefault("category", "OTHER")).toUpperCase()));
        item.setQuantity(toDouble(params.get("quantity"), 0.0));
        item.setUnit((String) params.get("unit"));
        item.setNotes((String) params.get("notes"));
        item.setLowStockThreshold(params.get("lowStockThreshold") != null
                ? toDouble(params.get("lowStockThreshold"), null) : null);

        item = inventoryItemRepository.save(item);
        log.info("Added inventory item '{}' for user {}", item.getName(), userId);

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("action", "add");
        result.put("item", toMap(item));
        result.put("message", "Added '" + item.getName() + "' to inventory");
        return result;
    }

    private Map<String, Object> updateItem(UUID userId, Map<String, Object> params) {
        UUID itemId = UUID.fromString((String) params.get("itemId"));
        InventoryItem item = inventoryItemRepository.findByIdAndUserId(itemId, userId)
                .orElseThrow(() -> new EntityNotFoundException("Inventory item not found: " + itemId));

        if (params.containsKey("quantity")) {
            item.setQuantity(toDouble(params.get("quantity"), item.getQuantity()));
        }
        if (params.containsKey("notes")) {
            item.setNotes((String) params.get("notes"));
        }
        if (params.containsKey("lowStockThreshold")) {
            item.setLowStockThreshold(toDouble(params.get("lowStockThreshold"), null));
        }

        item = inventoryItemRepository.save(item);
        log.info("Updated inventory item '{}' for user {}", item.getName(), userId);

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("action", "update");
        result.put("item", toMap(item));
        result.put("message", "Updated '" + item.getName() + "'");
        return result;
    }

    private Map<String, Object> deleteItem(UUID userId, Map<String, Object> params) {
        UUID itemId = UUID.fromString((String) params.get("itemId"));
        InventoryItem item = inventoryItemRepository.findByIdAndUserId(itemId, userId)
                .orElseThrow(() -> new EntityNotFoundException("Inventory item not found: " + itemId));

        inventoryItemRepository.delete(item);
        log.info("Deleted inventory item '{}' for user {}", item.getName(), userId);

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("action", "delete");
        result.put("message", "Deleted '" + item.getName() + "' from inventory");
        return result;
    }

    private Map<String, Object> lowStock(UUID userId) {
        List<InventoryItem> allItems = inventoryItemRepository.findByUserIdOrderByNameAsc(userId);
        List<InventoryItem> lowStockItems = allItems.stream()
                .filter(i -> i.getLowStockThreshold() != null
                        && i.getQuantity() <= i.getLowStockThreshold())
                .toList();

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("action", "low-stock");
        result.put("items", lowStockItems.stream().map(this::toMap).toList());
        result.put("message", lowStockItems.isEmpty()
                ? "No items below low-stock threshold"
                : lowStockItems.size() + " items are running low");
        return result;
    }

    private Map<String, Object> toMap(InventoryItem item) {
        Map<String, Object> map = new LinkedHashMap<>();
        map.put("id", item.getId().toString());
        map.put("name", item.getName());
        map.put("category", item.getCategory().name());
        map.put("quantity", item.getQuantity());
        map.put("unit", item.getUnit());
        map.put("notes", item.getNotes());
        map.put("lowStockThreshold", item.getLowStockThreshold());
        return map;
    }

    private Double toDouble(Object value, Double defaultValue) {
        if (value == null) return defaultValue;
        if (value instanceof Number n) return n.doubleValue();
        try {
            return Double.parseDouble(value.toString());
        } catch (NumberFormatException e) {
            return defaultValue;
        }
    }
}
