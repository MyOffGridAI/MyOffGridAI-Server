package com.myoffgridai.skills.builtin;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.myoffgridai.ai.dto.OllamaChatRequest;
import com.myoffgridai.ai.dto.OllamaChatResponse;
import com.myoffgridai.ai.dto.OllamaMessage;
import com.myoffgridai.ai.service.OllamaService;
import com.myoffgridai.common.exception.EntityNotFoundException;
import com.myoffgridai.config.AppConstants;
import com.myoffgridai.skills.model.PlannedTask;
import com.myoffgridai.skills.model.TaskStatus;
import com.myoffgridai.skills.repository.PlannedTaskRepository;
import com.myoffgridai.skills.service.BuiltInSkill;
import com.myoffgridai.system.service.SystemConfigService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Component;

import java.util.*;

/**
 * Built-in skill that decomposes goals into step-by-step action plans
 * using Ollama inference, and manages planned task lifecycle.
 */
@Component
public class TaskPlannerSkill implements BuiltInSkill {

    private static final Logger log = LoggerFactory.getLogger(TaskPlannerSkill.class);

    private final PlannedTaskRepository plannedTaskRepository;
    private final OllamaService ollamaService;
    private final ObjectMapper objectMapper;
    private final SystemConfigService systemConfigService;

    /**
     * Constructs the task planner skill.
     *
     * @param plannedTaskRepository the planned task repository
     * @param ollamaService         the Ollama service for inference
     * @param objectMapper          the JSON object mapper
     * @param systemConfigService   the system config service for dynamic AI settings
     */
    public TaskPlannerSkill(PlannedTaskRepository plannedTaskRepository,
                             OllamaService ollamaService,
                             ObjectMapper objectMapper,
                             SystemConfigService systemConfigService) {
        this.plannedTaskRepository = plannedTaskRepository;
        this.ollamaService = ollamaService;
        this.objectMapper = objectMapper;
        this.systemConfigService = systemConfigService;
    }

    @Override
    public String getSkillName() {
        return AppConstants.SKILL_TASK_PLANNER;
    }

    /**
     * Executes a task planning action.
     *
     * @param userId the user's ID
     * @param params must contain "action" key (plan, list, complete, cancel)
     * @return map with action, task data, and message
     */
    @Override
    public Map<String, Object> execute(UUID userId, Map<String, Object> params) {
        String action = (String) params.getOrDefault("action", "list");
        log.info("Task planner action '{}' for user: {}", action, userId);

        return switch (action) {
            case "plan" -> planTask(userId, params);
            case "complete" -> updateStatus(userId, params, TaskStatus.COMPLETED);
            case "cancel" -> updateStatus(userId, params, TaskStatus.CANCELLED);
            default -> listTasks(userId);
        };
    }

    private Map<String, Object> planTask(UUID userId, Map<String, Object> params) {
        String goal = (String) params.get("goal");
        if (goal == null || goal.isBlank()) {
            throw new IllegalArgumentException("Goal is required for planning");
        }

        String prompt = """
                Break down this goal into a practical step-by-step action plan:
                Goal: %s

                Consider available homestead resources and seasonal factors.
                Respond ONLY with JSON: {"title": "...", "steps": ["step1", "step2", ...], "estimatedResources": {"time": "...", "materials": ["..."]}}"""
                .formatted(goal);

        OllamaChatRequest request = new OllamaChatRequest(
                systemConfigService.getAiSettings().modelName(),
                List.of(new OllamaMessage("user", prompt)),
                false, Map.of());

        OllamaChatResponse response = ollamaService.chat(request);
        String responseText = response.message().content();

        PlannedTask task = new PlannedTask();
        task.setUserId(userId);
        task.setGoalDescription(goal);
        task.setStatus(TaskStatus.ACTIVE);

        try {
            Map<String, Object> parsed = objectMapper.readValue(
                    responseText, new TypeReference<>() {});
            task.setTitle((String) parsed.getOrDefault("title", goal));
            task.setSteps(objectMapper.writeValueAsString(parsed.get("steps")));
            task.setEstimatedResources(parsed.containsKey("estimatedResources")
                    ? objectMapper.writeValueAsString(parsed.get("estimatedResources")) : null);
        } catch (Exception e) {
            log.warn("Failed to parse task plan JSON, using raw response");
            task.setTitle(goal);
            task.setSteps("[\"" + responseText.replace("\"", "\\\"") + "\"]");
        }

        task = plannedTaskRepository.save(task);
        log.info("Created planned task '{}' for user {}", task.getTitle(), userId);

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("action", "plan");
        result.put("task", toMap(task));
        result.put("message", "Created task plan: " + task.getTitle());
        return result;
    }

    private Map<String, Object> listTasks(UUID userId) {
        var tasks = plannedTaskRepository.findByUserIdAndStatusOrderByCreatedAtDesc(
                userId, TaskStatus.ACTIVE, PageRequest.of(0, 50));

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("action", "list");
        result.put("tasks", tasks.getContent().stream().map(this::toMap).toList());
        result.put("message", "Found " + tasks.getTotalElements() + " active tasks");
        return result;
    }

    private Map<String, Object> updateStatus(UUID userId, Map<String, Object> params,
                                              TaskStatus newStatus) {
        UUID taskId = UUID.fromString((String) params.get("taskId"));
        PlannedTask task = plannedTaskRepository.findByIdAndUserId(taskId, userId)
                .orElseThrow(() -> new EntityNotFoundException("Planned task not found: " + taskId));

        task.setStatus(newStatus);
        task = plannedTaskRepository.save(task);
        log.info("Updated task '{}' status to {} for user {}", task.getTitle(), newStatus, userId);

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("action", newStatus == TaskStatus.COMPLETED ? "complete" : "cancel");
        result.put("task", toMap(task));
        result.put("message", "Task '" + task.getTitle() + "' marked as " + newStatus);
        return result;
    }

    private Map<String, Object> toMap(PlannedTask task) {
        Map<String, Object> map = new LinkedHashMap<>();
        map.put("id", task.getId().toString());
        map.put("title", task.getTitle());
        map.put("goalDescription", task.getGoalDescription());
        map.put("steps", task.getSteps());
        map.put("estimatedResources", task.getEstimatedResources());
        map.put("status", task.getStatus().name());
        map.put("createdAt", task.getCreatedAt() != null ? task.getCreatedAt().toString() : null);
        return map;
    }
}
