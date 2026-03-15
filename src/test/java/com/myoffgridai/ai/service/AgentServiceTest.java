package com.myoffgridai.ai.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.myoffgridai.ai.dto.AgentTaskResult;
import com.myoffgridai.ai.dto.OllamaChatResponse;
import com.myoffgridai.ai.dto.OllamaMessage;
import com.myoffgridai.skills.model.ExecutionStatus;
import com.myoffgridai.skills.model.Skill;
import com.myoffgridai.skills.model.SkillExecution;
import com.myoffgridai.skills.service.SkillExecutorService;
import com.myoffgridai.system.dto.AiSettingsDto;
import com.myoffgridai.system.service.SystemConfigService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Map;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AgentServiceTest {

    @Mock private OllamaService ollamaService;
    @Mock private SkillExecutorService skillExecutorService;
    @Mock private SystemConfigService systemConfigService;

    private AgentService agentService;
    private UUID userId;
    private UUID conversationId;

    @BeforeEach
    void setUp() {
        agentService = new AgentService(ollamaService, skillExecutorService,
                new ObjectMapper(), systemConfigService);
        userId = UUID.randomUUID();
        conversationId = UUID.randomUUID();

        lenient().when(systemConfigService.getAiSettings())
                .thenReturn(new AiSettingsDto("test-model", 0.7, 0.45, 5, 2048));
    }

    @Test
    void executeTask_noToolCalls_returnsSingleStepResult() {
        when(ollamaService.chat(any())).thenReturn(
                new OllamaChatResponse(
                        new OllamaMessage("assistant", "The answer is 42."),
                        true, 100L, 50));

        AgentTaskResult result = agentService.executeTask(userId, conversationId, "What is the meaning of life?");

        assertEquals(conversationId, result.conversationId());
        assertEquals("The answer is 42.", result.finalResponse());
        assertTrue(result.detectedToolCalls().isEmpty());
        assertEquals(1, result.stepCount());
        assertNotNull(result.completedAt());
    }

    @Test
    void executeTask_withToolCall_executesSkillAndContinues() {
        // First response contains a tool call
        String toolCallResponse = """
                Let me check the weather.
                {"tool": "weather-query", "params": {"location": "Portland"}}""";
        // Second response is the final answer
        String finalResponse = "The weather in Portland is sunny.";

        SkillExecution execution = createCompletedExecution("{\"hasSensorData\":false}");
        when(skillExecutorService.executeByName(eq("weather-query"), eq(userId), any()))
                .thenReturn(execution);

        when(ollamaService.chat(any()))
                .thenReturn(new OllamaChatResponse(
                        new OllamaMessage("assistant", toolCallResponse), true, 100L, 50))
                .thenReturn(new OllamaChatResponse(
                        new OllamaMessage("assistant", finalResponse), true, 100L, 50));

        AgentTaskResult result = agentService.executeTask(userId, conversationId, "What's the weather?");

        assertEquals(finalResponse, result.finalResponse());
        assertEquals(1, result.detectedToolCalls().size());
        assertEquals(2, result.stepCount());
        verify(skillExecutorService).executeByName(eq("weather-query"), eq(userId), any());
    }

    @Test
    void executeTask_toolCallFails_appendsErrorAndContinues() {
        String toolCallResponse = """
                {"tool": "inventory-tracker", "params": {"action": "list"}}""";
        String finalResponse = "Sorry, couldn't access inventory.";

        when(skillExecutorService.executeByName(eq("inventory-tracker"), eq(userId), any()))
                .thenThrow(new RuntimeException("DB error"));

        when(ollamaService.chat(any()))
                .thenReturn(new OllamaChatResponse(
                        new OllamaMessage("assistant", toolCallResponse), true, 100L, 50))
                .thenReturn(new OllamaChatResponse(
                        new OllamaMessage("assistant", finalResponse), true, 100L, 50));

        AgentTaskResult result = agentService.executeTask(userId, conversationId, "List my inventory");

        assertEquals(finalResponse, result.finalResponse());
        assertEquals(1, result.detectedToolCalls().size());
        assertEquals(2, result.stepCount());
    }

    @Test
    void executeTask_multipleToolCalls_executesAll() {
        String toolCallResponse = """
                I'll check your inventory and calculate food runway.
                {"tool": "inventory-tracker", "params": {"action": "list"}}
                {"tool": "resource-calculator", "params": {"calculationType": "food-runway"}}""";
        String finalResponse = "You have 10 days of food.";

        SkillExecution exec1 = createCompletedExecution("{\"items\":[]}");
        SkillExecution exec2 = createCompletedExecution("{\"daysOfSupply\":10}");
        when(skillExecutorService.executeByName(eq("inventory-tracker"), eq(userId), any()))
                .thenReturn(exec1);
        when(skillExecutorService.executeByName(eq("resource-calculator"), eq(userId), any()))
                .thenReturn(exec2);

        when(ollamaService.chat(any()))
                .thenReturn(new OllamaChatResponse(
                        new OllamaMessage("assistant", toolCallResponse), true, 100L, 50))
                .thenReturn(new OllamaChatResponse(
                        new OllamaMessage("assistant", finalResponse), true, 100L, 50));

        AgentTaskResult result = agentService.executeTask(userId, conversationId, "Check food");

        assertEquals(2, result.detectedToolCalls().size());
        verify(skillExecutorService).executeByName(eq("inventory-tracker"), eq(userId), any());
        verify(skillExecutorService).executeByName(eq("resource-calculator"), eq(userId), any());
    }

    @Test
    void executeTask_respectsMaxIterations() {
        // Always returns a tool call to test iteration limit
        String alwaysToolCall = """
                {"tool": "weather-query", "params": {"location": "test"}}""";

        SkillExecution execution = createCompletedExecution("{}");
        when(skillExecutorService.executeByName(anyString(), eq(userId), any()))
                .thenReturn(execution);

        when(ollamaService.chat(any())).thenReturn(
                new OllamaChatResponse(
                        new OllamaMessage("assistant", alwaysToolCall), true, 100L, 50));

        AgentTaskResult result = agentService.executeTask(userId, conversationId, "infinite loop");

        // Should stop at AGENT_MAX_ITERATIONS (5)
        assertEquals(5, result.stepCount());
        assertEquals(5, result.detectedToolCalls().size());
    }

    private SkillExecution createCompletedExecution(String outputResult) {
        SkillExecution execution = new SkillExecution();
        execution.setId(UUID.randomUUID());
        execution.setStatus(ExecutionStatus.COMPLETED);
        execution.setOutputResult(outputResult);
        Skill skill = new Skill();
        skill.setId(UUID.randomUUID());
        skill.setName("test");
        execution.setSkill(skill);
        return execution;
    }
}
