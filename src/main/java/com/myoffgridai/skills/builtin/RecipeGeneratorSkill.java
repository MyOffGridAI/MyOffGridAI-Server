package com.myoffgridai.skills.builtin;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.myoffgridai.ai.dto.OllamaChatRequest;
import com.myoffgridai.ai.dto.OllamaChatResponse;
import com.myoffgridai.ai.dto.OllamaMessage;
import com.myoffgridai.ai.service.OllamaService;
import com.myoffgridai.config.AppConstants;
import com.myoffgridai.skills.model.InventoryCategory;
import com.myoffgridai.skills.model.InventoryItem;
import com.myoffgridai.skills.repository.InventoryItemRepository;
import com.myoffgridai.skills.service.BuiltInSkill;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.util.*;
import java.util.stream.Collectors;

/**
 * Built-in skill that generates recipes based on the user's current
 * food inventory, using Ollama for creative recipe generation.
 */
@Component
public class RecipeGeneratorSkill implements BuiltInSkill {

    private static final Logger log = LoggerFactory.getLogger(RecipeGeneratorSkill.class);

    private final InventoryItemRepository inventoryItemRepository;
    private final OllamaService ollamaService;
    private final ObjectMapper objectMapper;

    /**
     * Constructs the recipe generator skill.
     *
     * @param inventoryItemRepository the inventory item repository
     * @param ollamaService           the Ollama service for inference
     * @param objectMapper            the JSON object mapper
     */
    public RecipeGeneratorSkill(InventoryItemRepository inventoryItemRepository,
                                 OllamaService ollamaService,
                                 ObjectMapper objectMapper) {
        this.inventoryItemRepository = inventoryItemRepository;
        this.ollamaService = ollamaService;
        this.objectMapper = objectMapper;
    }

    @Override
    public String getSkillName() {
        return AppConstants.SKILL_RECIPE_GENERATOR;
    }

    /**
     * Generates a recipe based on current food inventory.
     *
     * @param userId the user's ID
     * @param params optional: dietaryNeeds, cookingMethod, additionalIngredients
     * @return map with recipe details
     */
    @Override
    public Map<String, Object> execute(UUID userId, Map<String, Object> params) {
        log.info("Generating recipe for user: {}", userId);

        List<InventoryItem> foodItems = inventoryItemRepository.findByUserIdAndCategory(
                userId, InventoryCategory.FOOD);

        String inventoryList = foodItems.isEmpty()
                ? "No ingredients currently tracked in inventory."
                : foodItems.stream()
                .map(i -> i.getName() + " (" + i.getQuantity() + " " +
                        (i.getUnit() != null ? i.getUnit() : "units") + ")")
                .collect(Collectors.joining(", "));

        String dietaryNeeds = (String) params.getOrDefault("dietaryNeeds", "");
        String cookingMethod = (String) params.getOrDefault("cookingMethod", "");
        String additionalIngredients = (String) params.getOrDefault("additionalIngredients", "");

        String prompt = """
                You are a homestead cooking assistant. Generate a recipe using primarily these available ingredients:
                %s

                %s%s%s
                Provide: recipe name, ingredients with quantities, step-by-step instructions, estimated time.
                Format as JSON with keys: recipeName, ingredients (array), instructions (array of steps), estimatedMinutes, servings.
                Respond ONLY with the JSON object, no extra text."""
                .formatted(
                        inventoryList,
                        dietaryNeeds.isBlank() ? "" : "Additional preferences: " + dietaryNeeds + "\n",
                        cookingMethod.isBlank() ? "" : "Cooking method preference: " + cookingMethod + "\n",
                        additionalIngredients.isBlank() ? "" : "Additional ingredients available: " + additionalIngredients + "\n"
                );

        OllamaChatRequest request = new OllamaChatRequest(
                AppConstants.OLLAMA_MODEL,
                List.of(new OllamaMessage("user", prompt)),
                false, Map.of());

        OllamaChatResponse response = ollamaService.chat(request);
        String responseText = response.message().content();

        Map<String, Object> result = new LinkedHashMap<>();
        try {
            Map<String, Object> parsed = objectMapper.readValue(
                    responseText, new TypeReference<>() {});
            result.putAll(parsed);
        } catch (Exception e) {
            log.warn("Failed to parse recipe JSON, returning raw text");
            result.put("recipeText", responseText);
        }

        result.put("usedInventoryItems", foodItems.stream()
                .map(InventoryItem::getName).toList());
        return result;
    }
}
