package com.myoffgridai.skills.builtin;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.myoffgridai.ai.dto.OllamaChatResponse;
import com.myoffgridai.ai.dto.OllamaMessage;
import com.myoffgridai.ai.service.OllamaService;
import com.myoffgridai.common.exception.EntityNotFoundException;
import com.myoffgridai.config.AppConstants;
import com.myoffgridai.skills.model.PlannedTask;
import com.myoffgridai.skills.model.TaskStatus;
import com.myoffgridai.skills.repository.PlannedTaskRepository;
import com.myoffgridai.system.dto.AiSettingsDto;
import com.myoffgridai.system.service.SystemConfigService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class TaskPlannerSkillTest {

    @Mock private PlannedTaskRepository plannedTaskRepository;
    @Mock private OllamaService ollamaService;
    @Mock private SystemConfigService systemConfigService;

    private TaskPlannerSkill skill;
    private UUID userId;

    @BeforeEach
    void setUp() {
        skill = new TaskPlannerSkill(plannedTaskRepository, ollamaService,
                new ObjectMapper(), systemConfigService);
        userId = UUID.randomUUID();

        lenient().when(systemConfigService.getAiSettings())
                .thenReturn(new AiSettingsDto("test-model", 0.7, 0.45, 5, 2048, 4096, 20));
    }

    @Test
    void getSkillName_returnsTaskPlanner() {
        assertEquals(AppConstants.SKILL_TASK_PLANNER, skill.getSkillName());
    }

    @Test
    void execute_planAction_createsTask() {
        String jsonResponse = """
                {"title":"Build Rain Barrel","steps":["Get barrel","Install"],"estimatedResources":{"time":"2 hours","materials":["barrel"]}}""";
        when(ollamaService.chat(any())).thenReturn(
                new OllamaChatResponse(new OllamaMessage("assistant", jsonResponse), true, 100L, 50));
        when(plannedTaskRepository.save(any(PlannedTask.class))).thenAnswer(i -> {
            PlannedTask saved = i.getArgument(0);
            saved.setId(UUID.randomUUID());
            return saved;
        });

        Map<String, Object> result = skill.execute(userId,
                Map.of("action", "plan", "goal", "Build a rain barrel system"));

        assertEquals("plan", result.get("action"));
        assertTrue(result.get("message").toString().contains("Build Rain Barrel"));
        verify(plannedTaskRepository).save(any(PlannedTask.class));
    }

    @Test
    void execute_planAction_missingGoal_throwsIllegalArgument() {
        assertThrows(IllegalArgumentException.class,
                () -> skill.execute(userId, Map.of("action", "plan")));
    }

    @Test
    void execute_planAction_invalidJson_usesRawResponse() {
        when(ollamaService.chat(any())).thenReturn(
                new OllamaChatResponse(new OllamaMessage("assistant", "not json"), true, 100L, 50));
        when(plannedTaskRepository.save(any(PlannedTask.class))).thenAnswer(i -> {
            PlannedTask saved = i.getArgument(0);
            saved.setId(UUID.randomUUID());
            return saved;
        });

        Map<String, Object> result = skill.execute(userId,
                Map.of("action", "plan", "goal", "test goal"));

        assertEquals("plan", result.get("action"));
        verify(plannedTaskRepository).save(any(PlannedTask.class));
    }

    @Test
    void execute_listAction_returnsActiveTasks() {
        PlannedTask task = createTask("Test Task");
        when(plannedTaskRepository.findByUserIdAndStatusOrderByCreatedAtDesc(
                eq(userId), eq(TaskStatus.ACTIVE), any(Pageable.class)))
                .thenReturn(new PageImpl<>(List.of(task)));

        Map<String, Object> result = skill.execute(userId, Map.of("action", "list"));

        assertEquals("list", result.get("action"));
        assertTrue(result.get("message").toString().contains("1 active"));
    }

    @Test
    void execute_completeAction_updatesStatus() {
        UUID taskId = UUID.randomUUID();
        PlannedTask task = createTask("Complete Me");
        task.setId(taskId);
        when(plannedTaskRepository.findByIdAndUserId(taskId, userId))
                .thenReturn(Optional.of(task));
        when(plannedTaskRepository.save(any())).thenAnswer(i -> i.getArgument(0));

        Map<String, Object> result = skill.execute(userId,
                Map.of("action", "complete", "taskId", taskId.toString()));

        assertEquals("complete", result.get("action"));
        assertEquals(TaskStatus.COMPLETED, task.getStatus());
    }

    @Test
    void execute_cancelAction_updatesStatus() {
        UUID taskId = UUID.randomUUID();
        PlannedTask task = createTask("Cancel Me");
        task.setId(taskId);
        when(plannedTaskRepository.findByIdAndUserId(taskId, userId))
                .thenReturn(Optional.of(task));
        when(plannedTaskRepository.save(any())).thenAnswer(i -> i.getArgument(0));

        Map<String, Object> result = skill.execute(userId,
                Map.of("action", "cancel", "taskId", taskId.toString()));

        assertEquals("cancel", result.get("action"));
        assertEquals(TaskStatus.CANCELLED, task.getStatus());
    }

    @Test
    void execute_completeAction_taskNotFound_throwsException() {
        UUID taskId = UUID.randomUUID();
        when(plannedTaskRepository.findByIdAndUserId(taskId, userId))
                .thenReturn(Optional.empty());

        assertThrows(EntityNotFoundException.class,
                () -> skill.execute(userId,
                        Map.of("action", "complete", "taskId", taskId.toString())));
    }

    @Test
    void execute_defaultAction_isList() {
        when(plannedTaskRepository.findByUserIdAndStatusOrderByCreatedAtDesc(
                eq(userId), eq(TaskStatus.ACTIVE), any(Pageable.class)))
                .thenReturn(new PageImpl<>(List.of()));

        Map<String, Object> result = skill.execute(userId, Map.of());

        assertEquals("list", result.get("action"));
    }

    private PlannedTask createTask(String title) {
        PlannedTask task = new PlannedTask();
        task.setId(UUID.randomUUID());
        task.setUserId(userId);
        task.setTitle(title);
        task.setGoalDescription("Test goal");
        task.setSteps("[\"step1\"]");
        task.setStatus(TaskStatus.ACTIVE);
        return task;
    }
}
