package com.myoffgridai.ai.service;

import com.myoffgridai.auth.model.Role;
import com.myoffgridai.auth.model.User;
import com.myoffgridai.common.util.TokenCounter;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class SystemPromptBuilderTest {

    private SystemPromptBuilder builder;
    private User testUser;

    @BeforeEach
    void setUp() {
        builder = new SystemPromptBuilder();
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
    void build_containsMemoryPlaceholder() {
        String prompt = builder.build(testUser, "Homestead");
        assertTrue(prompt.contains("<!-- MEMORY_CONTEXT -->"));
    }

    @Test
    void build_containsRagPlaceholder() {
        String prompt = builder.build(testUser, "Homestead");
        assertTrue(prompt.contains("<!-- RAG_CONTEXT -->"));
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
}
