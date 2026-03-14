package com.myoffgridai.skills.builtin;

import com.myoffgridai.config.AppConstants;
import com.myoffgridai.skills.model.InventoryCategory;
import com.myoffgridai.skills.model.InventoryItem;
import com.myoffgridai.skills.repository.InventoryItemRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ResourceCalculatorSkillTest {

    @Mock private InventoryItemRepository inventoryItemRepository;

    private ResourceCalculatorSkill skill;
    private UUID userId;

    @BeforeEach
    void setUp() {
        skill = new ResourceCalculatorSkill(inventoryItemRepository);
        userId = UUID.randomUUID();
    }

    @Test
    void getSkillName_returnsResourceCalculator() {
        assertEquals(AppConstants.SKILL_RESOURCE_CALCULATOR, skill.getSkillName());
    }

    @Test
    void execute_missingCalculationType_throwsIllegalArgument() {
        assertThrows(IllegalArgumentException.class,
                () -> skill.execute(userId, Map.of()));
    }

    @Test
    void execute_unknownCalculationType_throwsIllegalArgument() {
        assertThrows(IllegalArgumentException.class,
                () -> skill.execute(userId, Map.of("calculationType", "unknown")));
    }

    // ── Power Budget ─────────────────────────────────────────────────────

    @Test
    void powerBudget_surplus_returnsCorrectValues() {
        Map<String, Object> params = Map.of(
                "calculationType", "power-budget",
                "panelWatts", 1000.0,
                "batteryKwh", 5.0,
                "dailyUsageWatts", 2000.0,
                "sunHoursPerDay", 6.0
        );

        Map<String, Object> result = skill.execute(userId, params);

        assertEquals("power-budget", result.get("calculationType"));
        // 1000 * 6 * 0.85 = 5100 Wh production, usage 2000, surplus 3100
        assertEquals(5100L, result.get("dailyProductionWh"));
        assertEquals(3100L, result.get("surplusOrDeficitWh"));
        assertTrue(result.get("recommendation").toString().contains("surplus"));
    }

    @Test
    void powerBudget_deficit_returnsRecommendation() {
        Map<String, Object> params = Map.of(
                "calculationType", "power-budget",
                "panelWatts", 200.0,
                "batteryKwh", 2.0,
                "dailyUsageWatts", 5000.0,
                "sunHoursPerDay", 5.0
        );

        Map<String, Object> result = skill.execute(userId, params);

        long surplus = (long) result.get("surplusOrDeficitWh");
        assertTrue(surplus < 0);
        assertTrue(result.get("recommendation").toString().contains("deficit"));
    }

    @Test
    void powerBudget_balanced_returnsBalancedMessage() {
        // panelWatts * sunHours * efficiency = dailyUsageWatts
        // 1000 * 5 * 0.85 = 4250
        Map<String, Object> params = Map.of(
                "calculationType", "power-budget",
                "panelWatts", 1000.0,
                "batteryKwh", 5.0,
                "dailyUsageWatts", 4250.0,
                "sunHoursPerDay", 5.0
        );

        Map<String, Object> result = skill.execute(userId, params);

        assertEquals(0L, result.get("surplusOrDeficitWh"));
        assertTrue(result.get("recommendation").toString().contains("balanced"));
    }

    @Test
    void powerBudget_zeroUsage_returnsBatteryBackupZero() {
        Map<String, Object> params = Map.of(
                "calculationType", "power-budget",
                "panelWatts", 500.0,
                "batteryKwh", 5.0,
                "dailyUsageWatts", 0.0
        );

        Map<String, Object> result = skill.execute(userId, params);

        assertEquals(0.0, result.get("batteryBackupDays"));
    }

    // ── Water Supply ─────────────────────────────────────────────────────

    @Test
    void waterSupply_healthySupply_returnsCorrectValues() {
        Map<String, Object> params = Map.of(
                "calculationType", "water-supply",
                "tankGallons", 500.0,
                "dailyUsageGallons", 10.0,
                "peopleCount", 2
        );

        Map<String, Object> result = skill.execute(userId, params);

        assertEquals("water-supply", result.get("calculationType"));
        // 500 / (10*2) = 25 days
        assertEquals(25.0, result.get("daysOfSupply"));
        assertTrue(result.get("recommendation").toString().contains("healthy"));
    }

    @Test
    void waterSupply_adequate_returnsWarning() {
        Map<String, Object> params = Map.of(
                "calculationType", "water-supply",
                "tankGallons", 100.0,
                "dailyUsageGallons", 10.0,
                "peopleCount", 1
        );

        Map<String, Object> result = skill.execute(userId, params);

        // 100 / 10 = 10 days (between 7 and 14)
        assertEquals(10.0, result.get("daysOfSupply"));
        assertTrue(result.get("recommendation").toString().contains("adequate"));
    }

    @Test
    void waterSupply_critical_returnsUrgent() {
        Map<String, Object> params = Map.of(
                "calculationType", "water-supply",
                "tankGallons", 30.0,
                "dailyUsageGallons", 10.0,
                "peopleCount", 1
        );

        Map<String, Object> result = skill.execute(userId, params);

        // 30 / 10 = 3 days (< 7)
        assertEquals(3.0, result.get("daysOfSupply"));
        assertTrue(result.get("recommendation").toString().contains("critically low"));
    }

    // ── Food Runway ──────────────────────────────────────────────────────

    @Test
    void foodRunway_withPoundsAndCounts_calculatesCorrectly() {
        InventoryItem rice = createFoodItem("Rice", 10.0, "lb");
        InventoryItem cans = createFoodItem("Beans", 20.0, "count");

        when(inventoryItemRepository.findByUserIdAndCategory(userId, InventoryCategory.FOOD))
                .thenReturn(List.of(rice, cans));

        Map<String, Object> params = Map.of(
                "calculationType", "food-runway",
                "peopleCount", 2,
                "dailyCaloriesPerPerson", 2000
        );

        Map<String, Object> result = skill.execute(userId, params);

        assertEquals("food-runway", result.get("calculationType"));
        // rice: 10 * 1500 = 15000, cans: 20 * 250 = 5000, total = 20000
        assertEquals(20000L, result.get("estimatedTotalCalories"));
        // 20000 / (2000 * 2) = 5 days
        assertEquals(5.0, result.get("daysOfSupply"));
        assertEquals(2, result.get("inventoryItemsAnalyzed"));
    }

    @Test
    void foodRunway_emptyInventory_returnsZeroDays() {
        when(inventoryItemRepository.findByUserIdAndCategory(userId, InventoryCategory.FOOD))
                .thenReturn(List.of());

        Map<String, Object> params = Map.of(
                "calculationType", "food-runway",
                "peopleCount", 1,
                "dailyCaloriesPerPerson", 2000
        );

        Map<String, Object> result = skill.execute(userId, params);

        assertEquals(0L, result.get("estimatedTotalCalories"));
        assertEquals(0.0, result.get("daysOfSupply"));
        assertEquals(0, result.get("inventoryItemsAnalyzed"));
    }

    @Test
    void foodRunway_usesDefaultParams() {
        when(inventoryItemRepository.findByUserIdAndCategory(userId, InventoryCategory.FOOD))
                .thenReturn(List.of());

        Map<String, Object> params = Map.of("calculationType", "food-runway");

        Map<String, Object> result = skill.execute(userId, params);

        assertEquals(2000, result.get("perPersonDailyCalories"));
    }

    private InventoryItem createFoodItem(String name, double quantity, String unit) {
        InventoryItem item = new InventoryItem();
        item.setId(UUID.randomUUID());
        item.setUserId(userId);
        item.setName(name);
        item.setCategory(InventoryCategory.FOOD);
        item.setQuantity(quantity);
        item.setUnit(unit);
        return item;
    }
}
