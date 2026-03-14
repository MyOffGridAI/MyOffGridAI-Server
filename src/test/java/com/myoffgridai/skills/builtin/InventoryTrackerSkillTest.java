package com.myoffgridai.skills.builtin;

import com.myoffgridai.common.exception.EntityNotFoundException;
import com.myoffgridai.config.AppConstants;
import com.myoffgridai.skills.model.InventoryCategory;
import com.myoffgridai.skills.model.InventoryItem;
import com.myoffgridai.skills.repository.InventoryItemRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.*;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class InventoryTrackerSkillTest {

    @Mock private InventoryItemRepository inventoryItemRepository;

    private InventoryTrackerSkill skill;
    private UUID userId;

    @BeforeEach
    void setUp() {
        skill = new InventoryTrackerSkill(inventoryItemRepository);
        userId = UUID.randomUUID();
    }

    @Test
    void getSkillName_returnsInventoryTracker() {
        assertEquals(AppConstants.SKILL_INVENTORY_TRACKER, skill.getSkillName());
    }

    @Test
    void execute_listAction_returnsAllItems() {
        InventoryItem item = createItem("Rice", InventoryCategory.FOOD, 10.0);
        when(inventoryItemRepository.findByUserIdOrderByNameAsc(userId))
                .thenReturn(List.of(item));

        Map<String, Object> result = skill.execute(userId, Map.of("action", "list"));

        assertEquals("list", result.get("action"));
        @SuppressWarnings("unchecked")
        List<Map<String, Object>> items = (List<Map<String, Object>>) result.get("items");
        assertEquals(1, items.size());
        assertEquals("Rice", items.get(0).get("name"));
    }

    @Test
    void execute_listWithCategory_returnsFiltered() {
        InventoryItem tool = createItem("Hammer", InventoryCategory.TOOLS, 1.0);
        when(inventoryItemRepository.findByUserIdAndCategory(userId, InventoryCategory.TOOLS))
                .thenReturn(List.of(tool));

        Map<String, Object> result = skill.execute(userId,
                Map.of("action", "list", "category", "tools"));

        @SuppressWarnings("unchecked")
        List<Map<String, Object>> items = (List<Map<String, Object>>) result.get("items");
        assertEquals(1, items.size());
        assertEquals("Hammer", items.get(0).get("name"));
    }

    @Test
    void execute_addAction_savesItem() {
        when(inventoryItemRepository.save(any(InventoryItem.class)))
                .thenAnswer(i -> {
                    InventoryItem saved = i.getArgument(0);
                    saved.setId(UUID.randomUUID());
                    return saved;
                });

        Map<String, Object> params = new HashMap<>();
        params.put("action", "add");
        params.put("name", "Flour");
        params.put("category", "FOOD");
        params.put("quantity", 5.0);
        params.put("unit", "lb");

        Map<String, Object> result = skill.execute(userId, params);

        assertEquals("add", result.get("action"));
        assertTrue(result.get("message").toString().contains("Flour"));
        verify(inventoryItemRepository).save(any(InventoryItem.class));
    }

    @Test
    void execute_updateAction_updatesItem() {
        UUID itemId = UUID.randomUUID();
        InventoryItem existing = createItem("Rice", InventoryCategory.FOOD, 10.0);
        existing.setId(itemId);
        when(inventoryItemRepository.findByIdAndUserId(itemId, userId))
                .thenReturn(Optional.of(existing));
        when(inventoryItemRepository.save(any())).thenAnswer(i -> i.getArgument(0));

        Map<String, Object> params = new HashMap<>();
        params.put("action", "update");
        params.put("itemId", itemId.toString());
        params.put("quantity", 20.0);

        Map<String, Object> result = skill.execute(userId, params);

        assertEquals("update", result.get("action"));
        assertEquals(20.0, existing.getQuantity());
    }

    @Test
    void execute_deleteAction_deletesItem() {
        UUID itemId = UUID.randomUUID();
        InventoryItem existing = createItem("Rice", InventoryCategory.FOOD, 10.0);
        existing.setId(itemId);
        when(inventoryItemRepository.findByIdAndUserId(itemId, userId))
                .thenReturn(Optional.of(existing));

        Map<String, Object> params = Map.of("action", "delete", "itemId", itemId.toString());
        Map<String, Object> result = skill.execute(userId, params);

        assertEquals("delete", result.get("action"));
        verify(inventoryItemRepository).delete(existing);
    }

    @Test
    void execute_deleteAction_notFound_throwsException() {
        UUID itemId = UUID.randomUUID();
        when(inventoryItemRepository.findByIdAndUserId(itemId, userId))
                .thenReturn(Optional.empty());

        assertThrows(EntityNotFoundException.class,
                () -> skill.execute(userId, Map.of("action", "delete", "itemId", itemId.toString())));
    }

    @Test
    void execute_lowStockAction_returnsLowStockItems() {
        InventoryItem low = createItem("Flour", InventoryCategory.FOOD, 1.0);
        low.setLowStockThreshold(5.0);
        InventoryItem ok = createItem("Rice", InventoryCategory.FOOD, 10.0);
        ok.setLowStockThreshold(5.0);

        when(inventoryItemRepository.findByUserIdOrderByNameAsc(userId))
                .thenReturn(List.of(low, ok));

        Map<String, Object> result = skill.execute(userId, Map.of("action", "low-stock"));

        assertEquals("low-stock", result.get("action"));
        @SuppressWarnings("unchecked")
        List<Map<String, Object>> items = (List<Map<String, Object>>) result.get("items");
        assertEquals(1, items.size());
        assertEquals("Flour", items.get(0).get("name"));
    }

    @Test
    void execute_lowStockAction_noLowStockItems_returnsEmptyWithMessage() {
        InventoryItem ok = createItem("Rice", InventoryCategory.FOOD, 10.0);
        ok.setLowStockThreshold(5.0);

        when(inventoryItemRepository.findByUserIdOrderByNameAsc(userId))
                .thenReturn(List.of(ok));

        Map<String, Object> result = skill.execute(userId, Map.of("action", "low-stock"));

        @SuppressWarnings("unchecked")
        List<Map<String, Object>> items = (List<Map<String, Object>>) result.get("items");
        assertTrue(items.isEmpty());
        assertTrue(result.get("message").toString().contains("No items"));
    }

    @Test
    void execute_defaultAction_isList() {
        when(inventoryItemRepository.findByUserIdOrderByNameAsc(userId))
                .thenReturn(List.of());

        Map<String, Object> result = skill.execute(userId, Map.of());

        assertEquals("list", result.get("action"));
    }

    private InventoryItem createItem(String name, InventoryCategory category, double quantity) {
        InventoryItem item = new InventoryItem();
        item.setId(UUID.randomUUID());
        item.setUserId(userId);
        item.setName(name);
        item.setCategory(category);
        item.setQuantity(quantity);
        return item;
    }
}
