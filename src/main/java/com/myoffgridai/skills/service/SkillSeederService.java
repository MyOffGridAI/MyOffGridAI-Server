package com.myoffgridai.skills.service;

import com.myoffgridai.config.AppConstants;
import com.myoffgridai.skills.model.Skill;
import com.myoffgridai.skills.model.SkillCategory;
import com.myoffgridai.skills.repository.SkillRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * Seeds built-in skill definitions into the database on application startup.
 *
 * <p>On each startup, checks if each built-in skill exists by name.
 * If not found, inserts the skill record with default metadata.</p>
 */
@Service
public class SkillSeederService {

    private static final Logger log = LoggerFactory.getLogger(SkillSeederService.class);

    private final SkillRepository skillRepository;

    /**
     * Constructs the skill seeder service.
     *
     * @param skillRepository the skill repository
     */
    public SkillSeederService(SkillRepository skillRepository) {
        this.skillRepository = skillRepository;
    }

    /**
     * Seeds all built-in skills on application startup.
     */
    @EventListener(ApplicationReadyEvent.class)
    @Transactional
    public void seedBuiltInSkills() {
        log.info("Checking built-in skill definitions...");
        int seeded = 0;

        List<SkillDefinition> definitions = List.of(
                new SkillDefinition(
                        AppConstants.SKILL_WEATHER_QUERY,
                        "Weather Query",
                        "Queries current weather conditions and forecasts. Stub implementation until sensor integration in Phase 6.",
                        SkillCategory.WEATHER,
                        "{\"type\":\"object\",\"properties\":{\"location\":{\"type\":\"string\"}}}"
                ),
                new SkillDefinition(
                        AppConstants.SKILL_INVENTORY_TRACKER,
                        "Inventory Tracker",
                        "Manages homestead inventory: add, update, delete, list, and low-stock alerts for food, tools, medical supplies, and more.",
                        SkillCategory.HOMESTEAD,
                        "{\"type\":\"object\",\"properties\":{\"action\":{\"type\":\"string\",\"enum\":[\"list\",\"add\",\"update\",\"delete\",\"low-stock\"]},\"name\":{\"type\":\"string\"},\"category\":{\"type\":\"string\"},\"quantity\":{\"type\":\"number\"},\"unit\":{\"type\":\"string\"}}}"
                ),
                new SkillDefinition(
                        AppConstants.SKILL_RECIPE_GENERATOR,
                        "Recipe Generator",
                        "Generates recipes based on current food inventory using AI inference. Considers dietary needs and cooking method preferences.",
                        SkillCategory.HOMESTEAD,
                        "{\"type\":\"object\",\"properties\":{\"dietaryNeeds\":{\"type\":\"string\"},\"cookingMethod\":{\"type\":\"string\"},\"additionalIngredients\":{\"type\":\"string\"}}}"
                ),
                new SkillDefinition(
                        AppConstants.SKILL_TASK_PLANNER,
                        "Task Planner",
                        "Decomposes goals into step-by-step action plans using AI inference. Manages planned task lifecycle: create, list, complete, cancel.",
                        SkillCategory.PLANNING,
                        "{\"type\":\"object\",\"properties\":{\"action\":{\"type\":\"string\",\"enum\":[\"plan\",\"list\",\"complete\",\"cancel\"]},\"goal\":{\"type\":\"string\"},\"taskId\":{\"type\":\"string\"}}}"
                ),
                new SkillDefinition(
                        AppConstants.SKILL_DOCUMENT_SUMMARIZER,
                        "Document Summarizer",
                        "Summarizes knowledge documents using AI inference. Generates executive summaries, key points, and action items.",
                        SkillCategory.KNOWLEDGE,
                        "{\"type\":\"object\",\"properties\":{\"documentId\":{\"type\":\"string\"}},\"required\":[\"documentId\"]}"
                ),
                new SkillDefinition(
                        AppConstants.SKILL_RESOURCE_CALCULATOR,
                        "Resource Calculator",
                        "Performs off-grid resource calculations: power budgets, water supply estimates, and food runway analysis. Pure math, no AI inference required.",
                        SkillCategory.RESOURCE,
                        "{\"type\":\"object\",\"properties\":{\"calculationType\":{\"type\":\"string\",\"enum\":[\"power-budget\",\"water-supply\",\"food-runway\"]},\"panelWatts\":{\"type\":\"number\"},\"batteryKwh\":{\"type\":\"number\"},\"dailyUsageWatts\":{\"type\":\"number\"}}}"
                )
        );

        for (SkillDefinition def : definitions) {
            if (skillRepository.findByName(def.name()).isEmpty()) {
                Skill skill = new Skill();
                skill.setName(def.name());
                skill.setDisplayName(def.displayName());
                skill.setDescription(def.description());
                skill.setVersion("1.0.0");
                skill.setAuthor("MyOffGridAI");
                skill.setCategory(def.category());
                skill.setIsEnabled(true);
                skill.setIsBuiltIn(true);
                skill.setParametersSchema(def.parametersSchema());
                skillRepository.save(skill);
                seeded++;
                log.info("Seeded built-in skill: {}", def.name());
            }
        }

        log.info("Skill seeding complete. {} new skills seeded, {} total built-in definitions",
                seeded, definitions.size());
    }

    private record SkillDefinition(
            String name,
            String displayName,
            String description,
            SkillCategory category,
            String parametersSchema
    ) {
    }
}
