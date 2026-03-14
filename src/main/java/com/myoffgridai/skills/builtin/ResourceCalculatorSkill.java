package com.myoffgridai.skills.builtin;

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
 * Built-in skill for off-grid resource calculations: power budgets,
 * water supply estimates, and food runway analysis.
 *
 * <p>All calculations are pure math — no Ollama inference needed.</p>
 */
@Component
public class ResourceCalculatorSkill implements BuiltInSkill {

    private static final Logger log = LoggerFactory.getLogger(ResourceCalculatorSkill.class);
    private static final double SOLAR_EFFICIENCY = 0.85;
    private static final double CALORIES_PER_LB = 1500.0;
    private static final double CALORIES_PER_COUNT = 250.0;

    private final InventoryItemRepository inventoryItemRepository;

    /**
     * Constructs the resource calculator skill.
     *
     * @param inventoryItemRepository the inventory item repository
     */
    public ResourceCalculatorSkill(InventoryItemRepository inventoryItemRepository) {
        this.inventoryItemRepository = inventoryItemRepository;
    }

    @Override
    public String getSkillName() {
        return AppConstants.SKILL_RESOURCE_CALCULATOR;
    }

    /**
     * Executes a resource calculation.
     *
     * @param userId the user's ID
     * @param params must contain "calculationType" (power-budget, water-supply, food-runway)
     * @return map with calculation results
     */
    @Override
    public Map<String, Object> execute(UUID userId, Map<String, Object> params) {
        String calcType = (String) params.get("calculationType");
        if (calcType == null || calcType.isBlank()) {
            throw new IllegalArgumentException("calculationType parameter is required");
        }

        log.info("Resource calculation '{}' for user: {}", calcType, userId);

        return switch (calcType) {
            case "power-budget" -> calculatePowerBudget(params);
            case "water-supply" -> calculateWaterSupply(params);
            case "food-runway" -> calculateFoodRunway(userId, params);
            default -> throw new IllegalArgumentException("Unknown calculation type: " + calcType);
        };
    }

    private Map<String, Object> calculatePowerBudget(Map<String, Object> params) {
        double panelWatts = toDouble(params.get("panelWatts"), 0.0);
        double batteryKwh = toDouble(params.get("batteryKwh"), 0.0);
        double dailyUsageWatts = toDouble(params.get("dailyUsageWatts"), 0.0);
        double sunHours = toDouble(params.get("sunHoursPerDay"), 5.0);

        double dailyProductionWh = panelWatts * sunHours * SOLAR_EFFICIENCY;
        double surplusOrDeficit = dailyProductionWh - dailyUsageWatts;
        double batteryBackupDays = dailyUsageWatts > 0
                ? (batteryKwh * 1000) / dailyUsageWatts : 0.0;

        String recommendation;
        if (surplusOrDeficit > 0) {
            recommendation = "Your system produces a surplus of %.0f Wh/day. Consider adding battery storage or increasing usage capacity."
                    .formatted(surplusOrDeficit);
        } else if (surplusOrDeficit < 0) {
            recommendation = "Your system has a deficit of %.0f Wh/day. Consider adding %.0f watts of solar panels or reducing usage."
                    .formatted(Math.abs(surplusOrDeficit), Math.abs(surplusOrDeficit) / (sunHours * SOLAR_EFFICIENCY));
        } else {
            recommendation = "Your system is balanced between production and usage.";
        }

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("calculationType", "power-budget");
        result.put("dailyProductionWh", Math.round(dailyProductionWh));
        result.put("dailyUsageWh", Math.round(dailyUsageWatts));
        result.put("surplusOrDeficitWh", Math.round(surplusOrDeficit));
        result.put("batteryBackupDays", Math.round(batteryBackupDays * 10.0) / 10.0);
        result.put("recommendation", recommendation);
        return result;
    }

    private Map<String, Object> calculateWaterSupply(Map<String, Object> params) {
        double tankGallons = toDouble(params.get("tankGallons"), 0.0);
        double dailyUsage = toDouble(params.get("dailyUsageGallons"), 0.0);
        int peopleCount = toInt(params.get("peopleCount"), 1);

        double totalDailyUsage = dailyUsage * peopleCount;
        double daysOfSupply = totalDailyUsage > 0 ? tankGallons / totalDailyUsage : 0.0;
        double perPersonDaily = peopleCount > 0 ? dailyUsage : 0.0;

        String recommendation;
        if (daysOfSupply >= 14) {
            recommendation = "Water supply is healthy with %.0f days of reserves."
                    .formatted(daysOfSupply);
        } else if (daysOfSupply >= 7) {
            recommendation = "Water supply is adequate but consider increasing storage for emergencies.";
        } else {
            recommendation = "Water supply is critically low. Consider rationing or securing additional water sources.";
        }

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("calculationType", "water-supply");
        result.put("daysOfSupply", Math.round(daysOfSupply * 10.0) / 10.0);
        result.put("perPersonDailyGallons", perPersonDaily);
        result.put("recommendation", recommendation);
        return result;
    }

    private Map<String, Object> calculateFoodRunway(UUID userId, Map<String, Object> params) {
        int peopleCount = toInt(params.get("peopleCount"), 1);
        int dailyCalories = toInt(params.get("dailyCaloriesPerPerson"), 2000);

        List<InventoryItem> foodItems = inventoryItemRepository.findByUserIdAndCategory(
                userId, InventoryCategory.FOOD);

        double totalCalories = 0.0;
        for (InventoryItem item : foodItems) {
            String unit = item.getUnit() != null ? item.getUnit().toLowerCase() : "count";
            if (unit.contains("lb") || unit.contains("pound")) {
                totalCalories += item.getQuantity() * CALORIES_PER_LB;
            } else {
                totalCalories += item.getQuantity() * CALORIES_PER_COUNT;
            }
        }

        double totalDailyCalories = (double) dailyCalories * peopleCount;
        double daysOfSupply = totalDailyCalories > 0 ? totalCalories / totalDailyCalories : 0.0;

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("calculationType", "food-runway");
        result.put("estimatedTotalCalories", Math.round(totalCalories));
        result.put("daysOfSupply", Math.round(daysOfSupply * 10.0) / 10.0);
        result.put("perPersonDailyCalories", dailyCalories);
        result.put("inventoryItemsAnalyzed", foodItems.size());
        result.put("disclaimer", "Calorie estimates are approximate: "
                + CALORIES_PER_LB + " cal/lb for weight-based items, "
                + CALORIES_PER_COUNT + " cal each for count-based items.");
        return result;
    }

    private double toDouble(Object value, double defaultValue) {
        if (value == null) return defaultValue;
        if (value instanceof Number n) return n.doubleValue();
        try { return Double.parseDouble(value.toString()); }
        catch (NumberFormatException e) { return defaultValue; }
    }

    private int toInt(Object value, int defaultValue) {
        if (value == null) return defaultValue;
        if (value instanceof Number n) return n.intValue();
        try { return Integer.parseInt(value.toString()); }
        catch (NumberFormatException e) { return defaultValue; }
    }
}
