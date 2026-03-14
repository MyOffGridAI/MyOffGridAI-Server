package com.myoffgridai.knowledge.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.myoffgridai.auth.model.Role;
import com.myoffgridai.auth.model.User;
import com.myoffgridai.auth.service.AuthService;
import com.myoffgridai.auth.service.JwtService;
import com.myoffgridai.config.CaptivePortalRedirectFilter;
import com.myoffgridai.config.JwtAuthFilter;
import com.myoffgridai.config.TestSecurityConfig;
import com.myoffgridai.knowledge.dto.KnowledgeDocumentDto;
import com.myoffgridai.knowledge.dto.KnowledgeSearchResultDto;
import com.myoffgridai.knowledge.model.DocumentStatus;
import com.myoffgridai.knowledge.service.KnowledgeService;
import com.myoffgridai.knowledge.service.SemanticSearchService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.data.domain.PageImpl;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.web.servlet.MockMvc;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(KnowledgeController.class)
@Import(TestSecurityConfig.class)
class KnowledgeControllerTest {

    @Autowired private MockMvc mockMvc;
    @Autowired private ObjectMapper objectMapper;

    @MockBean private KnowledgeService knowledgeService;
    @MockBean private SemanticSearchService semanticSearchService;
    @MockBean private JwtService jwtService;
    @MockBean private JwtAuthFilter jwtAuthFilter;
    @MockBean private AuthService authService;
    @MockitoBean private CaptivePortalRedirectFilter captivePortalRedirectFilter;

    private User testUser;
    private UUID userId;

    @BeforeEach
    void setUp() {
        userId = UUID.randomUUID();
        testUser = new User();
        testUser.setId(userId);
        testUser.setUsername("testuser");
        testUser.setDisplayName("Test User");
        testUser.setRole(Role.ROLE_MEMBER);
        testUser.setPasswordHash("hash");
    }

    @Test
    void uploadDocument_returnsCreated() throws Exception {
        MockMultipartFile file = new MockMultipartFile(
                "file", "test.pdf", "application/pdf", "pdf content".getBytes());
        KnowledgeDocumentDto dto = createTestDto();
        when(knowledgeService.upload(eq(userId), any())).thenReturn(dto);

        mockMvc.perform(multipart("/api/knowledge")
                        .file(file)
                        .with(user(testUser)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.filename").value("test.pdf"))
                .andExpect(jsonPath("$.message").value("Document uploaded, processing started"));
    }

    @Test
    void uploadDocument_unauthenticated_returns401() throws Exception {
        MockMultipartFile file = new MockMultipartFile(
                "file", "test.pdf", "application/pdf", "pdf".getBytes());

        mockMvc.perform(multipart("/api/knowledge")
                        .file(file))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void listDocuments_returnsPaginated() throws Exception {
        KnowledgeDocumentDto dto = createTestDto();
        when(knowledgeService.listDocuments(eq(userId), any()))
                .thenReturn(new PageImpl<>(List.of(dto)));

        mockMvc.perform(get("/api/knowledge")
                        .with(user(testUser)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data").isArray())
                .andExpect(jsonPath("$.data[0].filename").value("test.pdf"));
    }

    @Test
    void getDocument_returnsDocument() throws Exception {
        UUID docId = UUID.randomUUID();
        KnowledgeDocumentDto dto = createTestDto();
        when(knowledgeService.getDocument(docId, userId)).thenReturn(dto);

        mockMvc.perform(get("/api/knowledge/" + docId)
                        .with(user(testUser)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.filename").value("test.pdf"));
    }

    @Test
    void updateDisplayName_returnsUpdated() throws Exception {
        UUID docId = UUID.randomUUID();
        KnowledgeDocumentDto dto = new KnowledgeDocumentDto(
                docId, "test.pdf", "My Doc", "application/pdf",
                1024, DocumentStatus.READY, null, 5, Instant.now(), Instant.now());
        when(knowledgeService.updateDisplayName(docId, userId, "My Doc")).thenReturn(dto);

        mockMvc.perform(put("/api/knowledge/" + docId + "/display-name")
                        .with(user(testUser))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"displayName\":\"My Doc\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.displayName").value("My Doc"));
    }

    @Test
    void updateDisplayName_blankName_returns400() throws Exception {
        UUID docId = UUID.randomUUID();

        mockMvc.perform(put("/api/knowledge/" + docId + "/display-name")
                        .with(user(testUser))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"displayName\":\"\"}"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void deleteDocument_returnsSuccess() throws Exception {
        UUID docId = UUID.randomUUID();

        mockMvc.perform(delete("/api/knowledge/" + docId)
                        .with(user(testUser)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true));

        verify(knowledgeService).deleteDocument(docId, userId);
    }

    @Test
    void retryProcessing_returnsSuccess() throws Exception {
        UUID docId = UUID.randomUUID();
        KnowledgeDocumentDto dto = createTestDto();
        when(knowledgeService.retryProcessing(docId, userId)).thenReturn(dto);

        mockMvc.perform(post("/api/knowledge/" + docId + "/retry")
                        .with(user(testUser)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true));
    }

    @Test
    void searchKnowledge_returnsResults() throws Exception {
        KnowledgeSearchResultDto result = new KnowledgeSearchResultDto(
                UUID.randomUUID(), UUID.randomUUID(), "Guide",
                "content", 1, 0, 0.95f);
        when(semanticSearchService.search(eq(userId), eq("solar"), anyInt()))
                .thenReturn(List.of(result));

        mockMvc.perform(post("/api/knowledge/search")
                        .with(user(testUser))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"query\":\"solar\",\"topK\":5}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data[0].documentName").value("Guide"))
                .andExpect(jsonPath("$.data[0].similarityScore").value(0.95));
    }

    @Test
    void searchKnowledge_blankQuery_returns400() throws Exception {
        mockMvc.perform(post("/api/knowledge/search")
                        .with(user(testUser))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"query\":\"\"}"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void searchKnowledge_defaultTopK_uses5() throws Exception {
        when(semanticSearchService.search(eq(userId), anyString(), eq(5)))
                .thenReturn(List.of());

        mockMvc.perform(post("/api/knowledge/search")
                        .with(user(testUser))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"query\":\"test\"}"))
                .andExpect(status().isOk());

        verify(semanticSearchService).search(userId, "test", 5);
    }

    private KnowledgeDocumentDto createTestDto() {
        return new KnowledgeDocumentDto(
                UUID.randomUUID(), "test.pdf", null, "application/pdf",
                1024, DocumentStatus.PENDING, null, 0, Instant.now(), null);
    }
}
