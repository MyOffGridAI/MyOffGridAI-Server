package com.myoffgridai.ai.service;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.myoffgridai.ai.dto.*;
import com.myoffgridai.config.AppConstants;
import com.myoffgridai.skills.model.SkillExecution;
import com.myoffgridai.system.service.SystemConfigService;
import com.myoffgridai.skills.service.SkillExecutorService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Agent service for task execution with real tool-call dispatch.
 *
 * <p>Sends agent-style prompts to Ollama, detects tool-call JSON patterns
 * in the response, executes them via {@link SkillExecutorService}, appends
 * results as SYSTEM messages, and loops until no tool calls remain or the
 * maximum iteration count is reached.</p>
 */
@Service
public class AgentService {

    private static final Logger log = LoggerFactory.getLogger(AgentService.class);
    private static final Pattern TOOL_CALL_PATTERN = Pattern.compile(
            "\\{\\s*\"tool\"\\s*:\\s*\"([^\"]+)\"\\s*,\\s*\"params\"\\s*:\\s*(\\{[^}]*})\\s*}");

    private final OllamaService ollamaService;
    private final SkillExecutorService skillExecutorService;
    private final ObjectMapper objectMapper;
    private final SystemConfigService systemConfigService;

    /**
     * Constructs the agent service.
     *
     * @param ollamaService        the Ollama integration service
     * @param skillExecutorService the skill executor for tool-call dispatch
     * @param objectMapper         the JSON object mapper
     * @param systemConfigService  the system config service for dynamic AI settings
     */
    public AgentService(OllamaService ollamaService,
                        SkillExecutorService skillExecutorService,
                        ObjectMapper objectMapper,
                        SystemConfigService systemConfigService) {
        this.ollamaService = ollamaService;
        this.skillExecutorService = skillExecutorService;
        this.objectMapper = objectMapper;
        this.systemConfigService = systemConfigService;
    }

    /**
     * Executes an agent task by sending a step-by-step prompt to Ollama,
     * detecting tool-call patterns, executing them via the skill framework,
     * and looping with enriched context up to {@link AppConstants#AGENT_MAX_ITERATIONS}.
     *
     * @param userId          the user's ID
     * @param conversationId  the conversation ID for context
     * @param taskDescription the task to execute
     * @return the agent task result with response and executed tool calls
     */
    public AgentTaskResult executeTask(UUID userId, UUID conversationId, String taskDescription) {
        log.info("Executing agent task for user: {}, conversation: {}", userId, conversationId);

        String systemPrompt = """
                You are an AI agent that solves tasks step by step.
                Think through the problem carefully and break it into steps.
                If you need to use a tool, output a JSON block in this format:
                {"tool": "tool_name", "params": {"key": "value"}}
                Available tools: weather-query, inventory-tracker, recipe-generator, \
                task-planner, document-summarizer, resource-calculator.
                After reasoning through all steps, provide your final answer.""";

        List<OllamaMessage> messages = new ArrayList<>();
        messages.add(new OllamaMessage("system", systemPrompt));
        messages.add(new OllamaMessage("user", taskDescription));

        List<String> executedToolCalls = new ArrayList<>();
        String lastResponse = "";
        int step = 0;

        for (int iteration = 0; iteration < AppConstants.AGENT_MAX_ITERATIONS; iteration++) {
            step++;

            OllamaChatRequest request = new OllamaChatRequest(
                    systemConfigService.getAiSettings().modelName(), List.copyOf(messages), false, Map.of());

            OllamaChatResponse response = ollamaService.chat(request);
            lastResponse = response.message().content();
            messages.add(new OllamaMessage("assistant", lastResponse));

            List<ToolCall> toolCalls = parseToolCalls(lastResponse);
            if (toolCalls.isEmpty()) {
                log.info("Agent completed after {} steps with no further tool calls", step);
                break;
            }

            for (ToolCall tc : toolCalls) {
                executedToolCalls.add(tc.raw());
                log.info("Executing tool call: {} with params: {}", tc.toolName(), tc.params());

                try {
                    SkillExecution execution = skillExecutorService.executeByName(
                            tc.toolName(), userId, tc.params());

                    String resultJson = execution.getOutputResult() != null
                            ? execution.getOutputResult() : "{}";
                    messages.add(new OllamaMessage("system",
                            "Tool result for " + tc.toolName() + ": " + resultJson));

                } catch (Exception e) {
                    log.warn("Tool call '{}' failed: {}", tc.toolName(), e.getMessage());
                    messages.add(new OllamaMessage("system",
                            "Tool error for " + tc.toolName() + ": " + e.getMessage()));
                }
            }
        }

        return new AgentTaskResult(
                conversationId,
                lastResponse,
                executedToolCalls,
                step,
                Instant.now()
        );
    }

    private List<ToolCall> parseToolCalls(String content) {
        List<ToolCall> toolCalls = new ArrayList<>();
        Matcher matcher = TOOL_CALL_PATTERN.matcher(content);
        while (matcher.find()) {
            String toolName = matcher.group(1);
            String paramsJson = matcher.group(2);
            Map<String, Object> params;
            try {
                params = objectMapper.readValue(paramsJson, new TypeReference<>() {});
            } catch (Exception e) {
                log.warn("Failed to parse tool params for {}: {}", toolName, e.getMessage());
                params = Map.of();
            }
            toolCalls.add(new ToolCall(matcher.group(), toolName, params));
        }
        return toolCalls;
    }

    private record ToolCall(String raw, String toolName, Map<String, Object> params) {
    }
}
