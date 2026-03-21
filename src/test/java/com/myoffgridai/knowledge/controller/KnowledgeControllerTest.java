package com.myoffgridai.knowledge.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.myoffgridai.auth.model.Role;
import com.myoffgridai.auth.model.User;
import com.myoffgridai.auth.service.AuthService;
import com.myoffgridai.auth.service.JwtService;
import com.myoffgridai.config.CaptivePortalRedirectFilter;
import com.myoffgridai.config.JwtAuthFilter;
import com.myoffgridai.config.TestSecurityConfig;
import com.myoffgridai.knowledge.dto.DocumentContentDto;
import com.myoffgridai.knowledge.dto.KnowledgeDocumentDto;
import com.myoffgridai.knowledge.dto.KnowledgeSearchResultDto;
import com.myoffgridai.knowledge.model.DocumentStatus;
import com.myoffgridai.knowledge.model.KnowledgeDocument;
import com.myoffgridai.knowledge.service.FileStorageService;
import com.myoffgridai.knowledge.service.KnowledgeService;
import com.myoffgridai.knowledge.service.SemanticSearchService;
import com.myoffgridai.system.model.SystemConfig;
import com.myoffgridai.system.service.SystemConfigService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.data.domain.PageImpl;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.web.servlet.MockMvc;

import java.io.ByteArrayInputStream;
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

    @MockitoBean private KnowledgeService knowledgeService;
    @MockitoBean private SemanticSearchService semanticSearchService;
    @MockitoBean private SystemConfigService systemConfigService;
    @MockitoBean private FileStorageService fileStorageService;
    @MockitoBean private JwtService jwtService;
    @MockitoBean private JwtAuthFilter jwtAuthFilter;
    @MockitoBean private AuthService authService;
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

        SystemConfig config = new SystemConfig();
        when(systemConfigService.getConfig()).thenReturn(config);
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
        when(knowledgeService.listDocuments(eq(userId), eq("MINE"), any()))
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
                1024, DocumentStatus.READY, null, 5, Instant.now(), Instant.now(),
                true, false, false, true, null);
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

    // ── New endpoint tests ──────────────────────────────────────────────────

    @Test
    void downloadDocument_returnsFile() throws Exception {
        UUID docId = UUID.randomUUID();
        KnowledgeDocument doc = new KnowledgeDocument();
        doc.setId(docId);
        doc.setFilename("test.pdf");
        doc.setMimeType("application/pdf");
        doc.setStoragePath("/path/to/test.pdf");
        when(knowledgeService.getDocumentForDownload(docId, userId)).thenReturn(doc);
        when(fileStorageService.getInputStream("/path/to/test.pdf"))
                .thenReturn(new ByteArrayInputStream("pdf bytes".getBytes()));

        mockMvc.perform(get("/api/knowledge/" + docId + "/download")
                        .with(user(testUser)))
                .andExpect(status().isOk())
                .andExpect(header().string("Content-Type", "application/pdf"))
                .andExpect(header().exists("Content-Disposition"));
    }

    @Test
    void getDocumentContent_returnsContent() throws Exception {
        UUID docId = UUID.randomUUID();
        DocumentContentDto contentDto = new DocumentContentDto(
                docId, "My Doc", "[{\"insert\":\"hello\\n\"}]", "text/plain", true);
        when(knowledgeService.getDocumentContent(docId, userId)).thenReturn(contentDto);

        mockMvc.perform(get("/api/knowledge/" + docId + "/content")
                        .with(user(testUser)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.title").value("My Doc"))
                .andExpect(jsonPath("$.data.editable").value(true));
    }

    @Test
    void createDocument_returnsCreated() throws Exception {
        KnowledgeDocumentDto dto = createTestDto();
        when(knowledgeService.createFromEditor(eq(userId), eq("My Note"), anyString()))
                .thenReturn(dto);

        mockMvc.perform(post("/api/knowledge/create")
                        .with(user(testUser))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"title\":\"My Note\",\"content\":\"[{\\\"insert\\\":\\\"test\\\\n\\\"}]\"}"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.message").value("Document created, processing started"));
    }

    @Test
    void createDocument_blankTitle_returns400() throws Exception {
        mockMvc.perform(post("/api/knowledge/create")
                        .with(user(testUser))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"title\":\"\",\"content\":\"[{\\\"insert\\\":\\\"x\\\"}]\"}"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void createDocument_blankContent_returns400() throws Exception {
        mockMvc.perform(post("/api/knowledge/create")
                        .with(user(testUser))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"title\":\"Title\",\"content\":\"\"}"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void updateDocumentContent_returnsUpdated() throws Exception {
        UUID docId = UUID.randomUUID();
        KnowledgeDocumentDto dto = createTestDto();
        when(knowledgeService.updateContent(eq(docId), eq(userId), anyString()))
                .thenReturn(dto);

        mockMvc.perform(put("/api/knowledge/" + docId + "/content")
                        .with(user(testUser))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"content\":\"[{\\\"insert\\\":\\\"updated\\\\n\\\"}]\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.message").value("Content updated, re-processing started"));
    }

    @Test
    void updateDocumentContent_blankContent_returns400() throws Exception {
        UUID docId = UUID.randomUUID();

        mockMvc.perform(put("/api/knowledge/" + docId + "/content")
                        .with(user(testUser))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"content\":\"\"}"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void updateSharing_ownerCanShare_returnsOk() throws Exception {
        UUID docId = UUID.randomUUID();
        KnowledgeDocumentDto dto = createTestDto();
        when(knowledgeService.updateSharing(docId, userId, true)).thenReturn(dto);

        mockMvc.perform(patch("/api/knowledge/" + docId + "/sharing")
                        .with(user(testUser))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"shared\":true}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.message").value("Sharing updated"));
    }

    @Test
    void updateSharing_unauthenticated_returns401() throws Exception {
        UUID docId = UUID.randomUUID();

        mockMvc.perform(patch("/api/knowledge/" + docId + "/sharing")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"shared\":true}"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void listDocuments_scopeShared_passesToService() throws Exception {
        when(knowledgeService.listDocuments(eq(userId), eq("SHARED"), any()))
                .thenReturn(new PageImpl<>(List.of()));

        mockMvc.perform(get("/api/knowledge")
                        .param("scope", "SHARED")
                        .with(user(testUser)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true));

        verify(knowledgeService).listDocuments(eq(userId), eq("SHARED"), any());
    }

    @Test
    void listDocuments_defaultScope_usesMine() throws Exception {
        KnowledgeDocumentDto dto = createTestDto();
        when(knowledgeService.listDocuments(eq(userId), eq("MINE"), any()))
                .thenReturn(new PageImpl<>(List.of(dto)));

        mockMvc.perform(get("/api/knowledge")
                        .with(user(testUser)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true));

        verify(knowledgeService).listDocuments(eq(userId), eq("MINE"), any());
    }

    @Test
    void getDocument_sharedDocNonOwner_succeeds() throws Exception {
        UUID docId = UUID.randomUUID();
        KnowledgeDocumentDto dto = new KnowledgeDocumentDto(
                docId, "shared.pdf", "Shared Doc", "application/pdf",
                1024, DocumentStatus.READY, null, 5, Instant.now(), Instant.now(),
                true, false, true, false, "Other User");
        when(knowledgeService.getDocument(docId, userId)).thenReturn(dto);

        mockMvc.perform(get("/api/knowledge/" + docId)
                        .with(user(testUser)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.isShared").value(true))
                .andExpect(jsonPath("$.data.isOwner").value(false))
                .andExpect(jsonPath("$.data.ownerDisplayName").value("Other User"));
    }

    private KnowledgeDocumentDto createTestDto() {
        return new KnowledgeDocumentDto(
                UUID.randomUUID(), "test.pdf", null, "application/pdf",
                1024, DocumentStatus.PENDING, null, 0, Instant.now(), null,
                false, false, false, true, null);
    }
}
