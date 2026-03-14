package com.myoffgridai.integration;

import com.fasterxml.jackson.databind.ObjectMapper;
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

class MemoryIntegrationTest extends BaseIntegrationTest {

    @Autowired private MockMvc mockMvc;
    @Autowired private ObjectMapper objectMapper;

    @MockBean private OllamaService ollamaService;
    @MockBean private EmbeddingService embeddingService;

    private String accessToken;
    private String userBToken;

    @BeforeEach
    void setUp() throws Exception {
        when(ollamaService.isAvailable()).thenReturn(false);

        // Mock embeddings
        float[] mockEmbedding = new float[768];
        mockEmbedding[0] = 0.5f;
        when(embeddingService.embed(anyString())).thenReturn(mockEmbedding);
        when(embeddingService.embedAndFormat(anyString())).thenReturn("[0.5]");
        when(embeddingService.cosineSimilarity(any(), any())).thenReturn(0.9f);

        accessToken = registerAndLogin("memuser_" + System.nanoTime(), "pass");
        userBToken = registerAndLogin("memuser_b_" + System.nanoTime(), "pass");
    }

    @Test
    void fullLifecycle_listMemories_empty() throws Exception {
        mockMvc.perform(get("/api/memory")
                        .header("Authorization", "Bearer " + accessToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data").isArray());
    }

    @Test
    void getMemory_notFound_returns404() throws Exception {
        mockMvc.perform(get("/api/memory/" + java.util.UUID.randomUUID())
                        .header("Authorization", "Bearer " + accessToken))
                .andExpect(status().isNotFound());
    }

    @Test
    void exportMemories_returnsEmptyForNewUser() throws Exception {
        mockMvc.perform(get("/api/memory/export")
                        .header("Authorization", "Bearer " + accessToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data").isArray());
    }

    @Test
    void memoryEndpoints_unauthenticated_returns401() throws Exception {
        mockMvc.perform(get("/api/memory"))
                .andExpect(status().isUnauthorized());

        mockMvc.perform(get("/api/memory/export"))
                .andExpect(status().isUnauthorized());

        mockMvc.perform(post("/api/memory/search")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"query\":\"test\",\"topK\":5}"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void searchMemories_authenticated_returns200() throws Exception {
        mockMvc.perform(post("/api/memory/search")
                        .header("Authorization", "Bearer " + accessToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"query\":\"test query\",\"topK\":5}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data").isArray());
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
