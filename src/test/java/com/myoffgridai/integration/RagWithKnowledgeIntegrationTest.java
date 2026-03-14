package com.myoffgridai.integration;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.myoffgridai.ai.service.OllamaService;
import com.myoffgridai.auth.dto.LoginRequest;
import com.myoffgridai.auth.dto.RegisterRequest;
import com.myoffgridai.knowledge.service.FileStorageService;
import com.myoffgridai.knowledge.service.OcrService;
import com.myoffgridai.memory.service.EmbeddingService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import java.util.List;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

class RagWithKnowledgeIntegrationTest extends BaseIntegrationTest {

    @Autowired private MockMvc mockMvc;
    @Autowired private ObjectMapper objectMapper;

    @MockBean private OllamaService ollamaService;
    @MockBean private EmbeddingService embeddingService;
    @MockBean private FileStorageService fileStorageService;
    @MockBean private OcrService ocrService;

    private String accessToken;

    @BeforeEach
    void setUp() throws Exception {
        when(ollamaService.isAvailable()).thenReturn(false);

        float[] mockEmbedding = new float[768];
        mockEmbedding[0] = 0.5f;
        when(embeddingService.embed(anyString())).thenReturn(mockEmbedding);
        when(embeddingService.embedAndFormat(anyString()))
                .thenReturn(com.myoffgridai.memory.service.EmbeddingService.formatEmbedding(mockEmbedding));
        when(embeddingService.embedBatch(any())).thenReturn(List.of(mockEmbedding));
        when(embeddingService.cosineSimilarity(any(), any())).thenReturn(0.9f);

        when(fileStorageService.store(any(UUID.class), any(), anyString()))
                .thenReturn("/tmp/test-file-" + UUID.randomUUID());

        accessToken = registerAndLogin("ragknowledge_" + System.nanoTime(), "pass");
    }

    @Test
    void uploadAndSearch_endToEndFlow() throws Exception {
        // Mock getInputStream so async processing can succeed
        when(fileStorageService.getInputStream(anyString()))
                .thenReturn(new java.io.ByteArrayInputStream(
                        "Solar panels require at least 6 hours of direct sunlight per day for optimal energy production.".getBytes()));

        // 1. Upload a text document
        MockMultipartFile file = new MockMultipartFile(
                "file", "solar-guide.txt", "text/plain",
                "Solar panels require at least 6 hours of direct sunlight per day for optimal energy production.".getBytes());

        MvcResult uploadResult = mockMvc.perform(multipart("/api/knowledge")
                        .file(file)
                        .header("Authorization", "Bearer " + accessToken))
                .andExpect(status().isCreated())
                .andReturn();

        // 2. List documents — should show the uploaded document
        mockMvc.perform(get("/api/knowledge")
                        .header("Authorization", "Bearer " + accessToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data").isArray());

        // 3. Search the knowledge base
        mockMvc.perform(post("/api/knowledge/search")
                        .header("Authorization", "Bearer " + accessToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"query\":\"solar panels sunlight\",\"topK\":5}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true));
    }

    @Test
    void userIsolation_cannotAccessOtherUsersDocuments() throws Exception {
        when(fileStorageService.getInputStream(anyString()))
                .thenReturn(new java.io.ByteArrayInputStream(
                        "Secret data for user A only that must remain private and secure from other users in the system.".getBytes()));

        // User A uploads a document
        MockMultipartFile file = new MockMultipartFile(
                "file", "private.txt", "text/plain",
                "Secret data for user A only that must remain private and secure from other users in the system.".getBytes());

        MvcResult uploadResult = mockMvc.perform(multipart("/api/knowledge")
                        .file(file)
                        .header("Authorization", "Bearer " + accessToken))
                .andExpect(status().isCreated())
                .andReturn();

        String docId = objectMapper.readTree(
                uploadResult.getResponse().getContentAsString())
                .path("data").path("id").asText();

        // User B tries to access User A's document
        String userBToken = registerAndLogin("userbrag_" + System.nanoTime(), "pass");

        mockMvc.perform(get("/api/knowledge/" + docId)
                        .header("Authorization", "Bearer " + userBToken))
                .andExpect(status().isNotFound());

        mockMvc.perform(delete("/api/knowledge/" + docId)
                        .header("Authorization", "Bearer " + userBToken))
                .andExpect(status().isNotFound());
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
