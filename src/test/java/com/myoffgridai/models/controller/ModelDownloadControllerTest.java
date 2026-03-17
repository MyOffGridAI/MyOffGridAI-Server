package com.myoffgridai.models.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.myoffgridai.auth.model.Role;
import com.myoffgridai.auth.model.User;
import com.myoffgridai.auth.service.AuthService;
import com.myoffgridai.auth.service.JwtService;
import com.myoffgridai.config.CaptivePortalRedirectFilter;
import com.myoffgridai.config.TestSecurityConfig;
import com.myoffgridai.models.dto.*;
import com.myoffgridai.models.service.ModelCatalogService;
import com.myoffgridai.models.service.ModelDownloadProgressRegistry;
import com.myoffgridai.models.service.ModelDownloadService;
import com.myoffgridai.system.service.SystemConfigService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.time.Instant;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.authentication;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Unit tests for {@link ModelDownloadController}.
 *
 * <p>Tests all catalog, download, and local model management endpoints
 * with authentication and role-based access control verification.</p>
 */
@WebMvcTest(ModelDownloadController.class)
@Import(TestSecurityConfig.class)
class ModelDownloadControllerTest {

    @Autowired private MockMvc mockMvc;
    @Autowired private ObjectMapper objectMapper;

    @MockitoBean private ModelCatalogService catalogService;
    @MockitoBean private ModelDownloadService downloadService;
    @MockitoBean private ModelDownloadProgressRegistry progressRegistry;
    @MockitoBean private JwtService jwtService;
    @MockitoBean private AuthService authService;
    @MockitoBean private UserDetailsService userDetailsService;
    @MockitoBean private CaptivePortalRedirectFilter captivePortalRedirectFilter;
    @MockitoBean private SystemConfigService systemConfigService;

    private User ownerUser;
    private User memberUser;

    @BeforeEach
    void setUp() {
        ownerUser = new User();
        ownerUser.setId(UUID.randomUUID());
        ownerUser.setUsername("owner");
        ownerUser.setDisplayName("Owner User");
        ownerUser.setRole(Role.ROLE_OWNER);
        ownerUser.setPasswordHash("$2a$10$dummy");
        ownerUser.setIsActive(true);

        memberUser = new User();
        memberUser.setId(UUID.randomUUID());
        memberUser.setUsername("member");
        memberUser.setDisplayName("Member User");
        memberUser.setRole(Role.ROLE_MEMBER);
        memberUser.setPasswordHash("$2a$10$dummy");
        memberUser.setIsActive(true);
    }

    // ── Catalog endpoints ───────────────────────────────────────────────────

    @Test
    void searchCatalog_authenticated_returns200() throws Exception {
        HfSearchResultDto result = new HfSearchResultDto(
                List.of(new HfModelDto(
                        "TheBloke/Llama-2-7B-GGUF", "Llama-2-7B-GGUF", "TheBloke",
                        50000, 300, List.of("gguf"), "text-generation",
                        false, Instant.now(), Collections.emptyList())),
                1);

        when(catalogService.searchModels(eq("llama"), eq("gguf"), eq(20))).thenReturn(result);

        mockMvc.perform(get("/api/models/catalog/search")
                        .param("q", "llama")
                        .param("format", "gguf")
                        .param("limit", "20")
                        .with(authentication(createAuth(ownerUser))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.totalCount").value(1))
                .andExpect(jsonPath("$.data.models[0].id").value("TheBloke/Llama-2-7B-GGUF"));
    }

    @Test
    void searchCatalog_unauthenticated_returns401() throws Exception {
        mockMvc.perform(get("/api/models/catalog/search")
                        .param("q", "llama"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void searchCatalog_memberRole_returns200() throws Exception {
        when(catalogService.searchModels(anyString(), anyString(), anyInt()))
                .thenReturn(new HfSearchResultDto(Collections.emptyList(), 0));

        mockMvc.perform(get("/api/models/catalog/search")
                        .param("q", "test")
                        .with(authentication(createAuth(memberUser))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true));
    }

    @Test
    void getModelDetails_authenticated_returns200() throws Exception {
        HfModelDto model = new HfModelDto(
                "TheBloke/Llama-2-7B-GGUF", "Llama-2-7B-GGUF", "TheBloke",
                50000, 300, List.of("gguf"), "text-generation",
                false, Instant.now(), Collections.emptyList());

        when(catalogService.getModelDetails("TheBloke/Llama-2-7B-GGUF")).thenReturn(model);

        mockMvc.perform(get("/api/models/catalog/TheBloke/Llama-2-7B-GGUF")
                        .with(authentication(createAuth(ownerUser))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.id").value("TheBloke/Llama-2-7B-GGUF"))
                .andExpect(jsonPath("$.data.author").value("TheBloke"));
    }

    @Test
    void getModelDetails_unauthenticated_returns401() throws Exception {
        mockMvc.perform(get("/api/models/catalog/TheBloke/Llama-2-7B-GGUF"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void getModelFiles_authenticated_returns200() throws Exception {
        List<HfModelFileDto> files = List.of(
                new HfModelFileDto("model-Q4_K_M.gguf", 4000000000L, "abc123"),
                new HfModelFileDto("model-Q5_K_M.gguf", 5000000000L, null));

        when(catalogService.getModelFiles("TheBloke/Llama-2-7B-GGUF")).thenReturn(files);

        mockMvc.perform(get("/api/models/catalog/TheBloke/Llama-2-7B-GGUF/files")
                        .with(authentication(createAuth(ownerUser))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data[0].rfilename").value("model-Q4_K_M.gguf"))
                .andExpect(jsonPath("$.data[1].rfilename").value("model-Q5_K_M.gguf"));
    }

    @Test
    void getModelFiles_unauthenticated_returns401() throws Exception {
        mockMvc.perform(get("/api/models/catalog/TheBloke/Llama-2-7B-GGUF/files"))
                .andExpect(status().isUnauthorized());
    }

    // ── Download endpoints (OWNER only) ─────────────────────────────────────

    @Test
    void startDownload_ownerRole_returns200() throws Exception {
        String downloadId = "dl-uuid-123";
        DownloadProgress progress = new DownloadProgress(
                downloadId, "author/model", "model.gguf", DownloadStatus.QUEUED,
                0, 0, 0.0, 0.0, 0, "/models/author/model/model.gguf", null);

        when(downloadService.startDownload("author/model", "model.gguf")).thenReturn(downloadId);
        when(downloadService.getProgress(downloadId)).thenReturn(Optional.of(progress));

        StartDownloadRequest request = new StartDownloadRequest("author/model", "model.gguf");

        mockMvc.perform(post("/api/models/download")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request))
                        .with(authentication(createAuth(ownerUser))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.downloadId").value(downloadId))
                .andExpect(jsonPath("$.data.targetPath").value("/models/author/model/model.gguf"));
    }

    @Test
    void startDownload_memberRole_returns403() throws Exception {
        StartDownloadRequest request = new StartDownloadRequest("author/model", "model.gguf");

        mockMvc.perform(post("/api/models/download")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request))
                        .with(authentication(createAuth(memberUser))))
                .andExpect(status().isForbidden());
    }

    @Test
    void startDownload_unauthenticated_returns401() throws Exception {
        StartDownloadRequest request = new StartDownloadRequest("author/model", "model.gguf");

        mockMvc.perform(post("/api/models/download")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void getAllDownloads_ownerRole_returns200() throws Exception {
        DownloadProgress progress = new DownloadProgress(
                "dl-1", "author/model", "model.gguf", DownloadStatus.DOWNLOADING,
                1024, 4096, 25.0, 1024.0, 3, "/models/model.gguf", null);

        when(downloadService.getAllDownloads()).thenReturn(List.of(progress));

        mockMvc.perform(get("/api/models/download")
                        .with(authentication(createAuth(ownerUser))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data[0].downloadId").value("dl-1"))
                .andExpect(jsonPath("$.data[0].status").value("DOWNLOADING"));
    }

    @Test
    void getAllDownloads_memberRole_returns403() throws Exception {
        mockMvc.perform(get("/api/models/download")
                        .with(authentication(createAuth(memberUser))))
                .andExpect(status().isForbidden());
    }

    @Test
    void getAllDownloads_unauthenticated_returns401() throws Exception {
        mockMvc.perform(get("/api/models/download"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void getDownloadProgress_ownerRole_returnsSseEmitter() throws Exception {
        SseEmitter emitter = new SseEmitter();
        when(progressRegistry.subscribe("dl-1")).thenReturn(emitter);

        mockMvc.perform(get("/api/models/download/dl-1/progress")
                        .accept(MediaType.TEXT_EVENT_STREAM)
                        .with(authentication(createAuth(ownerUser))))
                .andExpect(status().isOk());

        verify(progressRegistry).subscribe("dl-1");
    }

    @Test
    void getDownloadProgress_memberRole_returns403() throws Exception {
        mockMvc.perform(get("/api/models/download/dl-1/progress")
                        .accept(MediaType.TEXT_EVENT_STREAM)
                        .with(authentication(createAuth(memberUser))))
                .andExpect(status().isForbidden());
    }

    @Test
    void cancelDownload_ownerRole_returns200() throws Exception {
        mockMvc.perform(delete("/api/models/download/dl-1")
                        .with(authentication(createAuth(ownerUser))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true));

        verify(downloadService).cancelDownload("dl-1");
    }

    @Test
    void cancelDownload_memberRole_returns403() throws Exception {
        mockMvc.perform(delete("/api/models/download/dl-1")
                        .with(authentication(createAuth(memberUser))))
                .andExpect(status().isForbidden());
    }

    @Test
    void cancelDownload_unauthenticated_returns401() throws Exception {
        mockMvc.perform(delete("/api/models/download/dl-1"))
                .andExpect(status().isUnauthorized());
    }

    // ── Local model endpoints ───────────────────────────────────────────────

    @Test
    void listLocalModels_authenticated_returns200() throws Exception {
        List<LocalModelFileDto> locals = List.of(
                new LocalModelFileDto("model-Q4.gguf", "author/model", "gguf",
                        4000000000L, Instant.now(), false));

        when(downloadService.listLocalModels()).thenReturn(locals);

        mockMvc.perform(get("/api/models/local")
                        .with(authentication(createAuth(ownerUser))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data[0].filename").value("model-Q4.gguf"))
                .andExpect(jsonPath("$.data[0].format").value("gguf"));
    }

    @Test
    void listLocalModels_memberRole_returns200() throws Exception {
        when(downloadService.listLocalModels()).thenReturn(Collections.emptyList());

        mockMvc.perform(get("/api/models/local")
                        .with(authentication(createAuth(memberUser))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true));
    }

    @Test
    void listLocalModels_unauthenticated_returns401() throws Exception {
        mockMvc.perform(get("/api/models/local"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void deleteLocalModel_ownerRole_returns200() throws Exception {
        mockMvc.perform(delete("/api/models/local/model-Q4.gguf")
                        .with(authentication(createAuth(ownerUser))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true));

        verify(downloadService).deleteLocalModel("model-Q4.gguf");
    }

    @Test
    void deleteLocalModel_memberRole_returns403() throws Exception {
        mockMvc.perform(delete("/api/models/local/model-Q4.gguf")
                        .with(authentication(createAuth(memberUser))))
                .andExpect(status().isForbidden());
    }

    @Test
    void deleteLocalModel_unauthenticated_returns401() throws Exception {
        mockMvc.perform(delete("/api/models/local/model-Q4.gguf"))
                .andExpect(status().isUnauthorized());
    }

    private UsernamePasswordAuthenticationToken createAuth(User user) {
        return new UsernamePasswordAuthenticationToken(
                user, null, user.getAuthorities());
    }
}
