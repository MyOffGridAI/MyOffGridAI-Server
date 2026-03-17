package com.myoffgridai.enrichment.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.myoffgridai.auth.model.Role;
import com.myoffgridai.auth.model.User;
import com.myoffgridai.auth.service.AuthService;
import com.myoffgridai.auth.service.JwtService;
import com.myoffgridai.config.CaptivePortalRedirectFilter;
import com.myoffgridai.config.JwtAuthFilter;
import com.myoffgridai.config.TestSecurityConfig;
import com.myoffgridai.enrichment.dto.SearchResultDto;
import com.myoffgridai.enrichment.service.ClaudeApiService;
import com.myoffgridai.enrichment.service.WebFetchService;
import com.myoffgridai.enrichment.service.WebSearchService;
import com.myoffgridai.knowledge.dto.KnowledgeDocumentDto;
import com.myoffgridai.knowledge.model.DocumentStatus;
import com.myoffgridai.settings.service.ExternalApiSettingsService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Web layer tests for {@link EnrichmentController}.
 */
@WebMvcTest(EnrichmentController.class)
@Import(TestSecurityConfig.class)
class EnrichmentControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockitoBean
    private WebFetchService webFetchService;

    @MockitoBean
    private WebSearchService webSearchService;

    @MockitoBean
    private ClaudeApiService claudeApiService;

    @MockitoBean
    private ExternalApiSettingsService settingsService;

    @MockitoBean
    private JwtService jwtService;

    @MockitoBean
    private JwtAuthFilter jwtAuthFilter;

    @MockitoBean
    private AuthService authService;

    @MockitoBean
    private CaptivePortalRedirectFilter captivePortalRedirectFilter;

    private User testUser;

    @BeforeEach
    void setUp() {
        testUser = new User();
        testUser.setId(UUID.randomUUID());
        testUser.setUsername("testuser");
        testUser.setDisplayName("Test User");
        testUser.setRole(Role.ROLE_MEMBER);
        testUser.setPasswordHash("hash");
    }

    @Test
    void fetchUrl_returns200WithDocument() throws Exception {
        KnowledgeDocumentDto doc = new KnowledgeDocumentDto(
                UUID.randomUUID(), "page.txt", "Test Page",
                "application/x-quill-delta", 500L, DocumentStatus.PENDING,
                null, 0, Instant.now(), null, true, true
        );
        when(webFetchService.fetchAndStore(anyString(), any(UUID.class), anyBoolean()))
                .thenReturn(doc);

        mockMvc.perform(post("/api/enrichment/fetch-url")
                        .with(user(testUser))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"url\":\"https://example.com\",\"summarizeWithClaude\":false}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.displayName").value("Test Page"));
    }

    @Test
    void fetchUrl_returns401WhenUnauthenticated() throws Exception {
        mockMvc.perform(post("/api/enrichment/fetch-url")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"url\":\"https://example.com\",\"summarizeWithClaude\":false}"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void search_returns200WithResults() throws Exception {
        List<SearchResultDto> searchResults = List.of(
                new SearchResultDto("Result 1", "https://example.com/1", "Description 1", null)
        );
        when(webSearchService.search(anyString())).thenReturn(searchResults);

        mockMvc.perform(post("/api/enrichment/search")
                        .with(user(testUser))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"query\":\"test\",\"storeTopN\":0,\"summarizeWithClaude\":false}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.results[0].title").value("Result 1"));
    }

    @Test
    void search_returns401WhenUnauthenticated() throws Exception {
        mockMvc.perform(post("/api/enrichment/search")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"query\":\"test\",\"storeTopN\":0,\"summarizeWithClaude\":false}"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void getStatus_returns200() throws Exception {
        when(claudeApiService.isAvailable()).thenReturn(true);
        when(webSearchService.isAvailable()).thenReturn(false);
        when(settingsService.getMaxWebFetchSizeKb()).thenReturn(512);
        when(settingsService.getSearchResultLimit()).thenReturn(5);

        mockMvc.perform(get("/api/enrichment/status")
                        .with(user(testUser)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.claudeAvailable").value(true))
                .andExpect(jsonPath("$.data.braveAvailable").value(false))
                .andExpect(jsonPath("$.data.maxWebFetchSizeKb").value(512));
    }

    @Test
    void getStatus_returns401WhenUnauthenticated() throws Exception {
        mockMvc.perform(get("/api/enrichment/status"))
                .andExpect(status().isUnauthorized());
    }
}
