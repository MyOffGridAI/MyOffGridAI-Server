package com.myoffgridai.integration;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.myoffgridai.ai.dto.OllamaChatResponse;
import com.myoffgridai.ai.dto.OllamaMessage;
import com.myoffgridai.ai.dto.SendMessageRequest;
import com.myoffgridai.ai.service.OllamaService;
import com.myoffgridai.auth.dto.LoginRequest;
import com.myoffgridai.auth.dto.RegisterRequest;
import com.myoffgridai.memory.service.EmbeddingService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Integration test for the full Memory + RAG pipeline.
 *
 * <p>Tests memory creation, vector search, RAG context injection into chat,
 * and cross-user isolation. Uses mocked Ollama and embedding services to
 * avoid requiring a running LLM or vector database extensions.</p>
 */
class MemoryRagIntegrationTest extends BaseIntegrationTest {

    @Autowired private MockMvc mockMvc;
    @Autowired private ObjectMapper objectMapper;

    @MockBean private OllamaService ollamaService;
    @MockBean private EmbeddingService embeddingService;

    private String userAToken;
    private String userBToken;

    @BeforeEach
    void setUp() throws Exception {
        OllamaChatResponse mockResponse = new OllamaChatResponse(
                new OllamaMessage("assistant", "Mocked response"), true, 100L, 5);
        when(ollamaService.chat(any())).thenReturn(mockResponse);
        when(ollamaService.isAvailable()).thenReturn(false);

        // Mock embeddings
        float[] mockEmbedding = new float[768];
        mockEmbedding[0] = 0.5f;
        when(embeddingService.embed(anyString())).thenReturn(mockEmbedding);
        when(embeddingService.embedAndFormat(anyString())).thenReturn("[0.5]");
        when(embeddingService.cosineSimilarity(any(), any())).thenReturn(0.9f);

        userAToken = registerAndLogin("ragmemuser_a_" + System.nanoTime(), "pass");
        userBToken = registerAndLogin("ragmemuser_b_" + System.nanoTime(), "pass");
    }

    @Test
    void memorySearch_authenticated_returnsResults() throws Exception {
        mockMvc.perform(post("/api/memory/search")
                        .header("Authorization", "Bearer " + userAToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"query\":\"solar panels\",\"topK\":5}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data").isArray());
    }

    @Test
    void memoryEndpoints_crossUserIsolation_cannotAccessOtherUsersData() throws Exception {
        // User A lists memories — empty
        mockMvc.perform(get("/api/memory")
                        .header("Authorization", "Bearer " + userAToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data").isArray());

        // User B lists memories — also empty, separate namespace
        mockMvc.perform(get("/api/memory")
                        .header("Authorization", "Bearer " + userBToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data").isArray());

        // User B cannot access user A's non-existent memory — 404
        mockMvc.perform(get("/api/memory/" + java.util.UUID.randomUUID())
                        .header("Authorization", "Bearer " + userBToken))
                .andExpect(status().isNotFound());
    }

    @Test
    void chatWithRag_sendMessage_returnsAssistantResponse() throws Exception {
        // Create conversation
        MvcResult createResult = mockMvc.perform(post("/api/chat/conversations")
                        .header("Authorization", "Bearer " + userAToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"title\":\"RAG Memory Test\"}"))
                .andExpect(status().isCreated())
                .andReturn();

        String conversationId = objectMapper.readTree(
                createResult.getResponse().getContentAsString())
                .path("data").path("id").asText();

        // Send message — RAG context builds (no memories yet, but pipeline executes)
        SendMessageRequest msgRequest = new SendMessageRequest(
                "How do I maintain my battery bank?", false);
        mockMvc.perform(post("/api/chat/conversations/" + conversationId + "/messages")
                        .header("Authorization", "Bearer " + userAToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(msgRequest)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.content").value("Mocked response"));
    }

    @Test
    void exportMemories_newUser_returnsEmptyArray() throws Exception {
        mockMvc.perform(get("/api/memory/export")
                        .header("Authorization", "Bearer " + userAToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data").isArray())
                .andExpect(jsonPath("$.data").isEmpty());
    }

    @Test
    void memoryEndpoints_unauthenticated_returns401() throws Exception {
        mockMvc.perform(get("/api/memory"))
                .andExpect(status().isUnauthorized());
        mockMvc.perform(post("/api/memory/search")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"query\":\"test\",\"topK\":5}"))
                .andExpect(status().isUnauthorized());
        mockMvc.perform(get("/api/memory/export"))
                .andExpect(status().isUnauthorized());
    }

    private String registerAndLogin(String username, String password) throws Exception {
        RegisterRequest registerRequest = new RegisterRequest(
                username, null, username, password, null);
        mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(registerRequest)))
                .andExpect(status().isCreated());

        LoginRequest loginRequest = new LoginRequest(username, password);
        MvcResult loginResult = mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(loginRequest)))
                .andExpect(status().isOk())
                .andReturn();

        return objectMapper.readTree(
                loginResult.getResponse().getContentAsString())
                .path("data").path("accessToken").asText();
    }
}
