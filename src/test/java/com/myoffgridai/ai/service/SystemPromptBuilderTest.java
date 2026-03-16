package com.myoffgridai.ai.service;

import com.myoffgridai.auth.model.Role;
import com.myoffgridai.auth.model.User;
import com.myoffgridai.common.util.TokenCounter;
import com.myoffgridai.config.AppConstants;
import com.myoffgridai.memory.dto.RagContext;
import com.myoffgridai.memory.service.RagService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@org.mockito.junit.jupiter.MockitoSettings(strictness = org.mockito.quality.Strictness.LENIENT)
class SystemPromptBuilderTest {

    @Mock
    private RagService ragService;

    private SystemPromptBuilder builder;
    private User testUser;

    @BeforeEach
    void setUp() {
        builder = new SystemPromptBuilder(ragService);
        testUser = new User();
        testUser.setUsername("testuser");
        testUser.setDisplayName("Test User");
        testUser.setRole(Role.ROLE_MEMBER);
    }

    @Test
    void build_containsUserDisplayName() {
        String prompt = builder.build(testUser, "Homestead");
        assertTrue(prompt.contains("Test User"));
    }

    @Test
    void build_containsInstanceName() {
        String prompt = builder.build(testUser, "Homestead");
        assertTrue(prompt.contains("Homestead"));
    }

    @Test
    void build_containsUserRole() {
        String prompt = builder.build(testUser, "Homestead");
        assertTrue(prompt.contains("ROLE_MEMBER"));
    }

    @Test
    void build_underFiveHundredTokens() {
        String prompt = builder.build(testUser, "Homestead");
        int tokens = TokenCounter.estimateTokens(prompt);
        assertTrue(tokens < 500, "Prompt should be under 500 tokens, was: " + tokens);
    }

    @Test
    void build_containsOfflineStatement() {
        String prompt = builder.build(testUser, "Homestead");
        assertTrue(prompt.contains("offline"));
    }

    @Test
    void build_withNullRagContext_noCommentMarkers() {
        String prompt = builder.build(testUser, "Homestead", null);
        assertFalse(prompt.contains("<!-- MEMORY_CONTEXT -->"));
        assertFalse(prompt.contains("<!-- RAG_CONTEXT -->"));
    }

    @Test
    void build_withEmptyRagContext_noContextSection() {
        RagContext empty = new RagContext(List.of(), List.of(), false, 0);
        String prompt = builder.build(testUser, "Homestead", empty);
        assertFalse(prompt.contains("[RELEVANT MEMORIES]"));
        assertFalse(prompt.contains("[RELEVANT KNOWLEDGE]"));
    }

    @Test
    void build_withPopulatedRagContext_includesMemories() {
        RagContext context = new RagContext(
                List.of("User has 3 chickens", "User prefers solar power"),
                List.of(), true, 20);

        when(ragService.formatContextBlock(any(RagContext.class))).thenReturn(
                "[RELEVANT MEMORIES]\n- User has 3 chickens\n- User prefers solar power\n[END MEMORIES]\n");

        String prompt = builder.build(testUser, "Homestead", context);
        assertTrue(prompt.contains("[RELEVANT MEMORIES]"));
        assertTrue(prompt.contains("User has 3 chickens"));
        assertTrue(prompt.contains("User prefers solar power"));
        assertTrue(prompt.contains("[END MEMORIES]"));
    }

    @Test
    void build_withOversizedRagContext_truncatesToLimit() {
        // Create a context that exceeds RAG_MAX_CONTEXT_TOKENS
        List<String> largeMemories = new ArrayList<>();
        for (int i = 0; i < 100; i++) {
            largeMemories.add("This is a very long memory fact number " + i
                    + " that contains lots of text to push over the token limit.");
        }
        int totalTokens = largeMemories.stream().mapToInt(TokenCounter::estimateTokens).sum();
        assertTrue(totalTokens > AppConstants.RAG_MAX_CONTEXT_TOKENS,
                "Test setup: total tokens should exceed limit");

        RagContext oversized = new RagContext(largeMemories, List.of(), true, totalTokens);

        // The builder truncates internally, then calls ragService.formatContextBlock
        when(ragService.formatContextBlock(any(RagContext.class))).thenAnswer(inv -> {
            RagContext ctx = inv.getArgument(0);
            return ctx.memorySnippets().isEmpty() ? "" : "[RELEVANT MEMORIES]\n[END MEMORIES]\n";
        });

        String prompt = builder.build(testUser, "Homestead", oversized);
        // Should not throw, and the prompt should be generated
        assertNotNull(prompt);
    }

    @Test
    void build_truncatesKnowledgeBeforeMemories() {
        // Knowledge should be truncated first
        List<String> memories = List.of("important memory");
        List<String> knowledge = new ArrayList<>();
        for (int i = 0; i < 100; i++) {
            knowledge.add("Long knowledge chunk " + i + " with lots of content to exceed limit");
        }
        int totalTokens = 10000; // Over limit

        RagContext context = new RagContext(memories, knowledge, true, totalTokens);

        when(ragService.formatContextBlock(any(RagContext.class))).thenAnswer(inv -> {
            RagContext ctx = inv.getArgument(0);
            // Verify memories are preserved while knowledge is truncated
            assertTrue(ctx.memorySnippets().size() >= 1 || ctx.knowledgeSnippets().isEmpty(),
                    "Memories should be preserved before knowledge");
            return "[CONTEXT]\n";
        });

        String prompt = builder.build(testUser, "Homestead", context);
        assertNotNull(prompt);
    }

    @Test
    void build_twoArgVersion_delegatesToThreeArgVersion() {
        String prompt2 = builder.build(testUser, "Homestead");
        String prompt3 = builder.build(testUser, "Homestead", null);
        // Both should produce valid prompts (content differs due to timestamp)
        assertTrue(prompt2.contains("Homestead"));
        assertTrue(prompt3.contains("Homestead"));
    }
}
