package com.myoffgridai.ai.service;

import com.myoffgridai.ai.dto.*;
import com.myoffgridai.config.AppConstants;
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
 * Agent service foundation for task execution with tool-call pattern detection.
 *
 * <p>In Phase 2, this service sends agent-style prompts to Ollama and parses
 * responses for tool-call patterns. Detected tool calls are logged but not
 * executed — actual skill integration comes in Phase 5.</p>
 */
@Service
public class AgentService {

    private static final Logger log = LoggerFactory.getLogger(AgentService.class);
    private static final Pattern TOOL_CALL_PATTERN = Pattern.compile(
            "\\{\\s*\"tool\"\\s*:\\s*\"[^\"]+\"\\s*,\\s*\"params\"\\s*:\\s*\\{[^}]*}\\s*}");

    private final OllamaService ollamaService;

    /**
     * Constructs the agent service.
     *
     * @param ollamaService the Ollama integration service
     */
    public AgentService(OllamaService ollamaService) {
        this.ollamaService = ollamaService;
    }

    /**
     * Executes an agent task by sending a step-by-step prompt to Ollama
     * and parsing the response for tool-call patterns.
     *
     * <p>In Phase 2, detected tool calls are returned in the result but not
     * executed. Future phases will wire these to the skill execution framework.</p>
     *
     * @param userId          the user's ID
     * @param conversationId  the conversation ID for context
     * @param taskDescription the task to execute
     * @return the agent task result with response and detected tool calls
     */
    public AgentTaskResult executeTask(UUID userId, UUID conversationId, String taskDescription) {
        log.info("Executing agent task for user: {}, conversation: {}", userId, conversationId);

        String systemPrompt = """
                You are an AI agent that solves tasks step by step.
                Think through the problem carefully and break it into steps.
                If you need to use a tool, output a JSON block in this format:
                {"tool": "tool_name", "params": {"key": "value"}}
                After reasoning through all steps, provide your final answer.""";

        List<OllamaMessage> messages = List.of(
                new OllamaMessage("system", systemPrompt),
                new OllamaMessage("user", taskDescription)
        );

        OllamaChatRequest request = new OllamaChatRequest(
                AppConstants.OLLAMA_MODEL, messages, false, Map.of());

        OllamaChatResponse response = ollamaService.chat(request);
        String responseContent = response.message().content();

        // Parse tool calls from response
        List<String> detectedToolCalls = parseToolCalls(responseContent);

        if (!detectedToolCalls.isEmpty()) {
            log.info("Detected {} tool calls in agent response (not executed in Phase 2)",
                    detectedToolCalls.size());
            detectedToolCalls.forEach(tc -> log.debug("Tool call: {}", tc));
        }

        return new AgentTaskResult(
                conversationId,
                responseContent,
                detectedToolCalls,
                1,
                Instant.now()
        );
    }

    private List<String> parseToolCalls(String content) {
        List<String> toolCalls = new ArrayList<>();
        Matcher matcher = TOOL_CALL_PATTERN.matcher(content);
        while (matcher.find()) {
            toolCalls.add(matcher.group());
        }
        return toolCalls;
    }
}
