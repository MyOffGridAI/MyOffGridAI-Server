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
import org.springframework.test.context.bean.override.mockito.MockitoBean;
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

class KnowledgeIntegrationTest extends BaseIntegrationTest {

    @Autowired private MockMvc mockMvc;
    @Autowired private ObjectMapper objectMapper;

    @MockitoBean private OllamaService ollamaService;
    @MockitoBean private EmbeddingService embeddingService;
    @MockitoBean private FileStorageService fileStorageService;
    @MockitoBean private OcrService ocrService;

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

        accessToken = registerAndLogin("knowledgeuser_" + System.nanoTime(), "pass");
    }

    @Test
    void listDocuments_empty_returns200() throws Exception {
        mockMvc.perform(get("/api/knowledge")
                        .header("Authorization", "Bearer " + accessToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data").isArray())
                .andExpect(jsonPath("$.data").isEmpty());
    }

    @Test
    void getDocument_notFound_returns404() throws Exception {
        mockMvc.perform(get("/api/knowledge/" + UUID.randomUUID())
                        .header("Authorization", "Bearer " + accessToken))
                .andExpect(status().isNotFound());
    }

    @Test
    void deleteDocument_notFound_returns404() throws Exception {
        mockMvc.perform(delete("/api/knowledge/" + UUID.randomUUID())
                        .header("Authorization", "Bearer " + accessToken))
                .andExpect(status().isNotFound());
    }

    @Test
    void searchKnowledge_noDocuments_returnsEmpty() throws Exception {
        mockMvc.perform(post("/api/knowledge/search")
                        .header("Authorization", "Bearer " + accessToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"query\":\"solar panels\",\"topK\":5}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data").isArray());
    }

    @Test
    void knowledgeEndpoints_unauthenticated_returns401() throws Exception {
        mockMvc.perform(get("/api/knowledge"))
                .andExpect(status().isUnauthorized());

        mockMvc.perform(get("/api/knowledge/" + UUID.randomUUID()))
                .andExpect(status().isUnauthorized());

        MockMultipartFile file = new MockMultipartFile(
                "file", "test.txt", "text/plain", "content".getBytes());
        mockMvc.perform(multipart("/api/knowledge").file(file))
                .andExpect(status().isUnauthorized());

        mockMvc.perform(post("/api/knowledge/search")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"query\":\"test\",\"topK\":5}"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void uploadDocument_validTextFile_returns201() throws Exception {
        // Mock getInputStream so async processing can succeed
        when(fileStorageService.getInputStream(anyString()))
                .thenReturn(new java.io.ByteArrayInputStream(
                        "This is a test document with enough content to pass the minimum chunk size threshold for testing purposes.".getBytes()));

        MockMultipartFile file = new MockMultipartFile(
                "file", "test.txt", "text/plain",
                "This is a test document with enough content to pass the minimum chunk size threshold for testing purposes.".getBytes());

        mockMvc.perform(multipart("/api/knowledge")
                        .file(file)
                        .header("Authorization", "Bearer " + accessToken))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.filename").value("test.txt"));
    }

    @Test
    void uploadDocument_unsupportedType_returns400() throws Exception {
        MockMultipartFile file = new MockMultipartFile(
                "file", "test.exe", "application/x-msdownload", "content".getBytes());

        mockMvc.perform(multipart("/api/knowledge")
                        .file(file)
                        .header("Authorization", "Bearer " + accessToken))
                .andExpect(status().isBadRequest());
    }

    @Test
    void searchKnowledge_blankQuery_returns400() throws Exception {
        mockMvc.perform(post("/api/knowledge/search")
                        .header("Authorization", "Bearer " + accessToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"query\":\"\"}"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void retryProcessing_notFound_returns404() throws Exception {
        mockMvc.perform(post("/api/knowledge/" + UUID.randomUUID() + "/retry")
                        .header("Authorization", "Bearer " + accessToken))
                .andExpect(status().isNotFound());
    }

    @Test
    void updateDisplayName_notFound_returns404() throws Exception {
        mockMvc.perform(put("/api/knowledge/" + UUID.randomUUID() + "/display-name")
                        .header("Authorization", "Bearer " + accessToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"displayName\":\"New Name\"}"))
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
