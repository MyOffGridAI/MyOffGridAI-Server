package com.myoffgridai.ai.service;

import com.myoffgridai.auth.model.User;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;

/**
 * Builds the system prompt injected before every conversation with Ollama.
 *
 * <p>In Phase 2, the prompt is built from user context only. Memory snippets
 * and RAG results will be injected via placeholder comments in Phase 3.</p>
 */
@Service
public class SystemPromptBuilder {

    private static final Logger log = LoggerFactory.getLogger(SystemPromptBuilder.class);

    /**
     * Assembles a system prompt for the given user and AI instance name.
     *
     * <p>The prompt identifies the AI, states its offline/private nature,
     * includes the current date/time, user role and name, and leaves
     * placeholder comments for future memory and RAG context injection.</p>
     *
     * @param user         the authenticated user
     * @param instanceName the name of this MyOffGridAI instance
     * @return the assembled system prompt, kept under 500 tokens
     */
    public String build(User user, String instanceName) {
        log.debug("Building system prompt for user: {}, instance: {}", user.getUsername(), instanceName);

        String now = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm")
                .withZone(ZoneOffset.UTC)
                .format(Instant.now());

        return String.format("""
                You are %s, the MyOffGrid AI assistant for %s.
                You run entirely offline on a private device. All data stays on this device and is never shared.

                Current date/time (UTC): %s
                User: %s (role: %s)

                <!-- MEMORY_CONTEXT -->

                <!-- RAG_CONTEXT -->

                Be helpful, concise, and accurate. If you don't know something, say so.""",
                instanceName,
                user.getDisplayName(),
                now,
                user.getDisplayName(),
                user.getRole().name()
        );
    }
}
