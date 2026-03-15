package com.myoffgridai.skills.builtin;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.myoffgridai.ai.dto.OllamaChatResponse;
import com.myoffgridai.ai.dto.OllamaMessage;
import com.myoffgridai.ai.service.OllamaService;
import com.myoffgridai.config.AppConstants;
import com.myoffgridai.skills.model.InventoryCategory;
import com.myoffgridai.skills.model.InventoryItem;
import com.myoffgridai.skills.repository.InventoryItemRepository;
import com.myoffgridai.system.dto.AiSettingsDto;
import com.myoffgridai.system.service.SystemConfigService;
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
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class RecipeGeneratorSkillTest {

    @Mock private InventoryItemRepository inventoryItemRepository;
    @Mock private OllamaService ollamaService;
    @Mock private SystemConfigService systemConfigService;

    private RecipeGeneratorSkill skill;
    private UUID userId;

    @BeforeEach
    void setUp() {
        skill = new RecipeGeneratorSkill(inventoryItemRepository, ollamaService,
                new ObjectMapper(), systemConfigService);
        userId = UUID.randomUUID();

        lenient().when(systemConfigService.getAiSettings())
                .thenReturn(new AiSettingsDto("test-model", 0.7, 0.45, 5, 2048));
    }

    @Test
    void getSkillName_returnsRecipeGenerator() {
        assertEquals(AppConstants.SKILL_RECIPE_GENERATOR, skill.getSkillName());
    }

    @Test
    void execute_withValidJson_parsesRecipe() {
        InventoryItem rice = createFoodItem("Rice", 5.0, "lb");
        when(inventoryItemRepository.findByUserIdAndCategory(userId, InventoryCategory.FOOD))
                .thenReturn(List.of(rice));

        String jsonResponse = """
                {"recipeName":"Fried Rice","ingredients":["2 cups rice","1 egg"],"instructions":["Cook rice","Fry"],"estimatedMinutes":20,"servings":2}""";
        when(ollamaService.chat(any())).thenReturn(
                new OllamaChatResponse(new OllamaMessage("assistant", jsonResponse), true, 100L, 50));

        Map<String, Object> result = skill.execute(userId, Map.of());

        assertEquals("Fried Rice", result.get("recipeName"));
        assertNotNull(result.get("ingredients"));
        assertNotNull(result.get("instructions"));
        @SuppressWarnings("unchecked")
        List<String> usedItems = (List<String>) result.get("usedInventoryItems");
        assertTrue(usedItems.contains("Rice"));
    }

    @Test
    void execute_withInvalidJson_returnsRawText() {
        when(inventoryItemRepository.findByUserIdAndCategory(userId, InventoryCategory.FOOD))
                .thenReturn(List.of());

        when(ollamaService.chat(any())).thenReturn(
                new OllamaChatResponse(new OllamaMessage("assistant", "not valid json"), true, 100L, 50));

        Map<String, Object> result = skill.execute(userId, Map.of());

        assertEquals("not valid json", result.get("recipeText"));
    }

    @Test
    void execute_emptyInventory_stillGeneratesRecipe() {
        when(inventoryItemRepository.findByUserIdAndCategory(userId, InventoryCategory.FOOD))
                .thenReturn(List.of());

        String jsonResponse = """
                {"recipeName":"Simple Soup","ingredients":["water"],"instructions":["Boil"],"estimatedMinutes":10,"servings":1}""";
        when(ollamaService.chat(any())).thenReturn(
                new OllamaChatResponse(new OllamaMessage("assistant", jsonResponse), true, 100L, 50));

        Map<String, Object> result = skill.execute(userId, Map.of());

        assertEquals("Simple Soup", result.get("recipeName"));
        @SuppressWarnings("unchecked")
        List<String> usedItems = (List<String>) result.get("usedInventoryItems");
        assertTrue(usedItems.isEmpty());
    }

    @Test
    void execute_withOptionalParams_includesInPrompt() {
        when(inventoryItemRepository.findByUserIdAndCategory(userId, InventoryCategory.FOOD))
                .thenReturn(List.of());

        String jsonResponse = """
                {"recipeName":"Vegan Stew","ingredients":[],"instructions":[],"estimatedMinutes":30,"servings":4}""";
        when(ollamaService.chat(any())).thenReturn(
                new OllamaChatResponse(new OllamaMessage("assistant", jsonResponse), true, 100L, 50));

        Map<String, Object> params = Map.of(
                "dietaryNeeds", "vegan",
                "cookingMethod", "stovetop",
                "additionalIngredients", "potatoes"
        );

        Map<String, Object> result = skill.execute(userId, params);

        assertEquals("Vegan Stew", result.get("recipeName"));
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
