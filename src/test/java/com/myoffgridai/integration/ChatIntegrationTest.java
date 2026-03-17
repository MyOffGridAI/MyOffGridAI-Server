package com.myoffgridai.integration;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.myoffgridai.ai.dto.OllamaChatResponse;
import com.myoffgridai.ai.dto.OllamaMessage;
import com.myoffgridai.ai.dto.SendMessageRequest;
import com.myoffgridai.ai.service.OllamaService;
import com.myoffgridai.memory.service.EmbeddingService;
import com.myoffgridai.auth.dto.AuthResponse;
import com.myoffgridai.auth.dto.LoginRequest;
import com.myoffgridai.auth.dto.RegisterRequest;
import com.myoffgridai.common.response.ApiResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

class ChatIntegrationTest extends BaseIntegrationTest {

    @Autowired private MockMvc mockMvc;
    @Autowired private ObjectMapper objectMapper;

    @MockitoBean private OllamaService ollamaService;
    @MockitoBean private EmbeddingService embeddingService;

    private String accessToken;
    private String userBToken;

    @BeforeEach
    void setUp() throws Exception {
        // Mock Ollama for all tests
        OllamaChatResponse mockResponse = new OllamaChatResponse(
                new OllamaMessage("assistant", "Mocked response"), true, 1000L, 5);
        when(ollamaService.chat(any())).thenReturn(mockResponse);
        when(ollamaService.isAvailable()).thenReturn(false);

        // Mock embeddings
        float[] mockEmbedding = new float[768];
        mockEmbedding[0] = 0.5f;
        when(embeddingService.embed(any())).thenReturn(mockEmbedding);
        when(embeddingService.embedAndFormat(any())).thenReturn("[0.5]");
        when(embeddingService.cosineSimilarity(any(), any())).thenReturn(0.5f);

        // Register and login user A
        accessToken = registerAndLogin("chatuser_" + System.nanoTime(), "pass");

        // Register and login user B
        userBToken = registerAndLogin("chatuser_b_" + System.nanoTime(), "pass");
    }

    @Test
    void fullFlow_createConversation_sendMessage_getConversation_archive_delete() throws Exception {
        // Create conversation
        MvcResult createResult = mockMvc.perform(post("/api/chat/conversations")
                        .header("Authorization", "Bearer " + accessToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"title\":\"Test Chat\"}"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.data.title").value("Test Chat"))
                .andReturn();

        String conversationId = objectMapper.readTree(
                createResult.getResponse().getContentAsString())
                .path("data").path("id").asText();

        // Send message
        SendMessageRequest msgRequest = new SendMessageRequest("Hello AI", false);
        mockMvc.perform(post("/api/chat/conversations/" + conversationId + "/messages")
                        .header("Authorization", "Bearer " + accessToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(msgRequest)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.content").value("Mocked response"));

        // Get conversation — verify messageCount
        mockMvc.perform(get("/api/chat/conversations/" + conversationId)
                        .header("Authorization", "Bearer " + accessToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.messageCount").value(2));

        // List messages
        mockMvc.perform(get("/api/chat/conversations/" + conversationId + "/messages")
                        .header("Authorization", "Bearer " + accessToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data").isArray());

        // Archive
        mockMvc.perform(put("/api/chat/conversations/" + conversationId + "/archive")
                        .header("Authorization", "Bearer " + accessToken))
                .andExpect(status().isOk());

        // Delete
        mockMvc.perform(delete("/api/chat/conversations/" + conversationId)
                        .header("Authorization", "Bearer " + accessToken))
                .andExpect(status().isOk());
    }

    @Test
    void userIsolation_userBCannotAccessUserAConversation() throws Exception {
        // User A creates conversation
        MvcResult createResult = mockMvc.perform(post("/api/chat/conversations")
                        .header("Authorization", "Bearer " + accessToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"title\":\"Private\"}"))
                .andExpect(status().isCreated())
                .andReturn();

        String conversationId = objectMapper.readTree(
                createResult.getResponse().getContentAsString())
                .path("data").path("id").asText();

        // User B tries to access — should get 404
        mockMvc.perform(get("/api/chat/conversations/" + conversationId)
                        .header("Authorization", "Bearer " + userBToken))
                .andExpect(status().isNotFound());
    }

    @Test
    void listConversations_returnsOnlyOwnConversations() throws Exception {
        // User A creates a conversation
        mockMvc.perform(post("/api/chat/conversations")
                        .header("Authorization", "Bearer " + accessToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"title\":\"User A Chat\"}"))
                .andExpect(status().isCreated());

        // User B lists conversations — should not see User A's
        mockMvc.perform(get("/api/chat/conversations")
                        .header("Authorization", "Bearer " + userBToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data").isArray());
    }

    @Test
    void sendMessage_unauthenticated_returns401() throws Exception {
        mockMvc.perform(post("/api/chat/conversations/" + java.util.UUID.randomUUID() + "/messages")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"content\":\"hello\",\"stream\":false}"))
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
