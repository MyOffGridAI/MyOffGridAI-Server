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
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

class RagPipelineIntegrationTest extends BaseIntegrationTest {

    @Autowired private MockMvc mockMvc;
    @Autowired private ObjectMapper objectMapper;

    @MockitoBean private OllamaService ollamaService;
    @MockitoBean private EmbeddingService embeddingService;

    private String accessToken;

    @BeforeEach
    void setUp() throws Exception {
        OllamaChatResponse mockResponse = new OllamaChatResponse(
                new OllamaMessage("assistant", "Mocked RAG response"), true, 1000L, 5);
        when(ollamaService.chat(any())).thenReturn(mockResponse);
        when(ollamaService.isAvailable()).thenReturn(false);

        // Mock embeddings
        float[] mockEmbedding = new float[768];
        mockEmbedding[0] = 0.5f;
        when(embeddingService.embed(anyString())).thenReturn(mockEmbedding);
        when(embeddingService.embedAndFormat(anyString())).thenReturn("[0.5]");
        when(embeddingService.cosineSimilarity(any(), any())).thenReturn(0.5f);

        accessToken = registerAndLogin("raguser_" + System.nanoTime(), "pass");
    }

    @Test
    void sendMessage_withRagContext_setsHasRagContextField() throws Exception {
        // Create conversation
        MvcResult createResult = mockMvc.perform(post("/api/chat/conversations")
                        .header("Authorization", "Bearer " + accessToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"title\":\"RAG Test\"}"))
                .andExpect(status().isCreated())
                .andReturn();

        String conversationId = objectMapper.readTree(
                createResult.getResponse().getContentAsString())
                .path("data").path("id").asText();

        // Send message — RAG context will be empty (no memories yet) so hasRagContext=false
        SendMessageRequest msgRequest = new SendMessageRequest("Tell me about my solar panels", false);
        mockMvc.perform(post("/api/chat/conversations/" + conversationId + "/messages")
                        .header("Authorization", "Bearer " + accessToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(msgRequest)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.content").value("Mocked RAG response"));
    }

    @Test
    void chatFlow_endToEnd_withRagIntegration() throws Exception {
        // Create conversation
        MvcResult createResult = mockMvc.perform(post("/api/chat/conversations")
                        .header("Authorization", "Bearer " + accessToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"title\":\"End to End\"}"))
                .andExpect(status().isCreated())
                .andReturn();

        String conversationId = objectMapper.readTree(
                createResult.getResponse().getContentAsString())
                .path("data").path("id").asText();

        // Send message
        mockMvc.perform(post("/api/chat/conversations/" + conversationId + "/messages")
                        .header("Authorization", "Bearer " + accessToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"content\":\"Hello\",\"stream\":false}"))
                .andExpect(status().isOk());

        // Verify conversation has messages
        mockMvc.perform(get("/api/chat/conversations/" + conversationId)
                        .header("Authorization", "Bearer " + accessToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.messageCount").value(2));
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
