package com.myoffgridai.library.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.myoffgridai.auth.model.Role;
import com.myoffgridai.auth.model.User;
import com.myoffgridai.auth.service.AuthService;
import com.myoffgridai.auth.service.JwtService;
import com.myoffgridai.config.CaptivePortalRedirectFilter;
import com.myoffgridai.config.JwtAuthFilter;
import com.myoffgridai.config.TestSecurityConfig;
import com.myoffgridai.library.config.LibraryProperties;
import com.myoffgridai.library.dto.*;
import com.myoffgridai.library.model.Ebook;
import com.myoffgridai.library.model.EbookFormat;
import com.myoffgridai.library.service.EbookService;
import com.myoffgridai.library.service.GutenbergService;
import com.myoffgridai.library.service.KiwixCatalogService;
import com.myoffgridai.library.service.KiwixDownloadService;
import com.myoffgridai.library.service.KiwixProcessService;
import com.myoffgridai.library.service.ZimFileService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.data.domain.PageImpl;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Unit tests for {@link LibraryController}.
 */
@WebMvcTest(LibraryController.class)
@Import(TestSecurityConfig.class)
class LibraryControllerTest {

    @Autowired private MockMvc mockMvc;
    @Autowired private ObjectMapper objectMapper;

    @MockitoBean private ZimFileService zimFileService;
    @MockitoBean private EbookService ebookService;
    @MockitoBean private GutenbergService gutenbergService;
    @MockitoBean private KiwixProcessService kiwixProcessService;
    @MockitoBean private KiwixCatalogService kiwixCatalogService;
    @MockitoBean private KiwixDownloadService kiwixDownloadService;
    @MockitoBean private LibraryProperties libraryProperties;
    @MockitoBean private JwtService jwtService;
    @MockitoBean private JwtAuthFilter jwtAuthFilter;
    @MockitoBean private AuthService authService;
    @MockitoBean private CaptivePortalRedirectFilter captivePortalRedirectFilter;

    private User ownerUser;
    private User memberUser;
    private UUID ownerId;
    private UUID memberId;

    @BeforeEach
    void setUp() {
        ownerId = UUID.randomUUID();
        ownerUser = new User();
        ownerUser.setId(ownerId);
        ownerUser.setUsername("owner");
        ownerUser.setDisplayName("Owner");
        ownerUser.setRole(Role.ROLE_OWNER);
        ownerUser.setPasswordHash("hash");

        memberId = UUID.randomUUID();
        memberUser = new User();
        memberUser.setId(memberId);
        memberUser.setUsername("member");
        memberUser.setDisplayName("Member");
        memberUser.setRole(Role.ROLE_MEMBER);
        memberUser.setPasswordHash("hash");
    }

    // ── ZIM Endpoints ────────────────────────────────────────────────────────

    @Test
    void uploadZim_asOwner_returnsCreated() throws Exception {
        MockMultipartFile file = new MockMultipartFile(
                "file", "wikipedia.zim", "application/octet-stream", "zim data".getBytes());
        ZimFileDto dto = createZimFileDto();
        when(zimFileService.upload(any(), eq("Wikipedia"), eq("reference"), eq(ownerId))).thenReturn(dto);

        mockMvc.perform(multipart("/api/library/zim")
                        .file(file)
                        .param("displayName", "Wikipedia")
                        .param("category", "reference")
                        .with(user(ownerUser)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.filename").value("wikipedia.zim"));
    }

    @Test
    void uploadZim_asMember_returns403() throws Exception {
        MockMultipartFile file = new MockMultipartFile(
                "file", "wikipedia.zim", "application/octet-stream", "data".getBytes());

        mockMvc.perform(multipart("/api/library/zim")
                        .file(file)
                        .param("displayName", "Wikipedia")
                        .with(user(memberUser)))
                .andExpect(status().isForbidden());
    }

    @Test
    void uploadZim_unauthenticated_returns401() throws Exception {
        MockMultipartFile file = new MockMultipartFile(
                "file", "wikipedia.zim", "application/octet-stream", "data".getBytes());

        mockMvc.perform(multipart("/api/library/zim")
                        .file(file)
                        .param("displayName", "Wikipedia"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void listZimFiles_authenticated_returnsOk() throws Exception {
        when(zimFileService.listAll()).thenReturn(List.of(createZimFileDto()));

        mockMvc.perform(get("/api/library/zim")
                        .with(user(memberUser)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data").isArray())
                .andExpect(jsonPath("$.data[0].filename").value("wikipedia.zim"));
    }

    @Test
    void listZimFiles_unauthenticated_returns401() throws Exception {
        mockMvc.perform(get("/api/library/zim"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void deleteZim_asOwner_returnsOk() throws Exception {
        UUID id = UUID.randomUUID();

        mockMvc.perform(delete("/api/library/zim/" + id)
                        .with(user(ownerUser)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true));

        verify(zimFileService).delete(id);
    }

    @Test
    void deleteZim_asMember_returns403() throws Exception {
        UUID id = UUID.randomUUID();

        mockMvc.perform(delete("/api/library/zim/" + id)
                        .with(user(memberUser)))
                .andExpect(status().isForbidden());
    }

    @Test
    void kiwixStatus_authenticated_returnsOk() throws Exception {
        KiwixStatusDto status = new KiwixStatusDto(true, "http://localhost:8888", 3, true, "INSTALLED", null);
        when(zimFileService.getKiwixStatus()).thenReturn(status);

        mockMvc.perform(get("/api/library/kiwix/status")
                        .with(user(memberUser)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.available").value(true))
                .andExpect(jsonPath("$.data.bookCount").value(3))
                .andExpect(jsonPath("$.data.processManaged").value(true))
                .andExpect(jsonPath("$.data.installationStatus").value("INSTALLED"));
    }

    @Test
    void kiwixUrl_authenticated_returnsOk() throws Exception {
        when(zimFileService.getKiwixServeUrl()).thenReturn("http://localhost:8888");

        mockMvc.perform(get("/api/library/kiwix/url")
                        .with(user(memberUser)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data").value("http://localhost:8888"));
    }

    // ── eBook Endpoints ──────────────────────────────────────────────────────

    @Test
    void uploadEbook_asOwner_returnsCreated() throws Exception {
        MockMultipartFile file = new MockMultipartFile(
                "file", "book.epub", "application/epub+zip", "epub data".getBytes());
        EbookDto dto = createEbookDto();
        when(ebookService.upload(any(), eq("My Book"), eq("Author"), eq(ownerId))).thenReturn(dto);

        mockMvc.perform(multipart("/api/library/ebooks")
                        .file(file)
                        .param("title", "My Book")
                        .param("author", "Author")
                        .with(user(ownerUser)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.title").value("Test Book"));
    }

    @Test
    void uploadEbook_asMember_returns403() throws Exception {
        MockMultipartFile file = new MockMultipartFile(
                "file", "book.epub", "application/epub+zip", "data".getBytes());

        mockMvc.perform(multipart("/api/library/ebooks")
                        .file(file)
                        .param("title", "Book")
                        .with(user(memberUser)))
                .andExpect(status().isForbidden());
    }

    @Test
    void listEbooks_authenticated_returnsPaginated() throws Exception {
        EbookDto dto = createEbookDto();
        when(ebookService.list(any(), any(), any()))
                .thenReturn(new PageImpl<>(List.of(dto)));

        mockMvc.perform(get("/api/library/ebooks")
                        .with(user(memberUser)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data").isArray())
                .andExpect(jsonPath("$.data[0].title").value("Test Book"));
    }

    @Test
    void getEbook_authenticated_returnsOk() throws Exception {
        UUID id = UUID.randomUUID();
        EbookDto dto = createEbookDto();
        when(ebookService.get(id)).thenReturn(dto);

        mockMvc.perform(get("/api/library/ebooks/" + id)
                        .with(user(memberUser)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.title").value("Test Book"));
    }

    @Test
    void deleteEbook_asOwner_returnsOk() throws Exception {
        UUID id = UUID.randomUUID();

        mockMvc.perform(delete("/api/library/ebooks/" + id)
                        .with(user(ownerUser)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true));

        verify(ebookService).delete(id);
    }

    @Test
    void deleteEbook_asMember_returns403() throws Exception {
        UUID id = UUID.randomUUID();

        mockMvc.perform(delete("/api/library/ebooks/" + id)
                        .with(user(memberUser)))
                .andExpect(status().isForbidden());
    }

    @Test
    void downloadEbook_authenticated_returnsFile() throws Exception {
        UUID id = UUID.randomUUID();
        Ebook ebook = new Ebook();
        ebook.setId(id);
        ebook.setFormat(EbookFormat.EPUB);

        // Create a temp file for the download
        java.nio.file.Path tempFile = java.nio.file.Files.createTempFile("test", ".epub");
        java.nio.file.Files.writeString(tempFile, "epub content");
        ebook.setFilePath(tempFile.toString());

        when(ebookService.getForDownload(id)).thenReturn(ebook);

        mockMvc.perform(get("/api/library/ebooks/" + id + "/content")
                        .with(user(memberUser)))
                .andExpect(status().isOk())
                .andExpect(header().string("Content-Type", "application/epub+zip"));

        java.nio.file.Files.deleteIfExists(tempFile);
    }

    // ── Gutenberg Endpoints ──────────────────────────────────────────────────

    @Test
    void browseGutenberg_authenticated_returnsOk() throws Exception {
        GutenbergSearchResultDto result = new GutenbergSearchResultDto(
                2, null, null, List.of(
                new GutenbergBookDto(1342, "Pride and Prejudice",
                        List.of("Austen, Jane"), List.of("Fiction"),
                        List.of("en"), 80000,
                        Map.of("application/epub+zip", "https://gutenberg.org/1342.epub")),
                new GutenbergBookDto(84, "Frankenstein",
                        List.of("Shelley, Mary"), List.of("Science fiction"),
                        List.of("en"), 100000,
                        Map.of("application/epub+zip", "https://gutenberg.org/84.epub"))));
        when(gutenbergService.browse("popular", 10)).thenReturn(result);

        mockMvc.perform(get("/api/library/gutenberg/browse")
                        .with(user(memberUser)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.count").value(2))
                .andExpect(jsonPath("$.data.results[0].title").value("Pride and Prejudice"))
                .andExpect(jsonPath("$.data.results[1].title").value("Frankenstein"));
    }

    @Test
    void browseGutenberg_withSortParam_returnsOk() throws Exception {
        GutenbergSearchResultDto result = new GutenbergSearchResultDto(
                1, null, null, List.of(
                new GutenbergBookDto(99999, "Newest Book",
                        List.of("Modern Author"), List.of("Fiction"),
                        List.of("en"), 100,
                        Map.of())));
        when(gutenbergService.browse("descending", 5)).thenReturn(result);

        mockMvc.perform(get("/api/library/gutenberg/browse")
                        .param("sort", "descending")
                        .param("limit", "5")
                        .with(user(memberUser)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.count").value(1))
                .andExpect(jsonPath("$.data.results[0].title").value("Newest Book"));
    }

    @Test
    void browseGutenberg_unauthenticated_returns401() throws Exception {
        mockMvc.perform(get("/api/library/gutenberg/browse"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void searchGutenberg_authenticated_returnsOk() throws Exception {
        GutenbergSearchResultDto result = new GutenbergSearchResultDto(
                1, null, null, List.of(
                new GutenbergBookDto(1342, "Pride and Prejudice",
                        List.of("Austen, Jane"), List.of("Fiction"),
                        List.of("en"), 50000,
                        Map.of("application/epub+zip", "https://gutenberg.org/1342.epub"))));
        when(gutenbergService.search("pride", 20)).thenReturn(result);

        mockMvc.perform(get("/api/library/gutenberg/search")
                        .param("query", "pride")
                        .with(user(memberUser)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.count").value(1))
                .andExpect(jsonPath("$.data.results[0].title").value("Pride and Prejudice"));
    }

    @Test
    void searchGutenberg_unauthenticated_returns401() throws Exception {
        mockMvc.perform(get("/api/library/gutenberg/search")
                        .param("query", "test"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void getGutenbergBook_authenticated_returnsOk() throws Exception {
        GutenbergBookDto book = new GutenbergBookDto(
                84, "Frankenstein", List.of("Shelley, Mary"),
                List.of("Science fiction"), List.of("en"), 100000,
                Map.of("application/epub+zip", "https://gutenberg.org/84.epub"));
        when(gutenbergService.getBookMetadata(84)).thenReturn(book);

        mockMvc.perform(get("/api/library/gutenberg/84")
                        .with(user(memberUser)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.title").value("Frankenstein"));
    }

    @Test
    void importGutenberg_asOwner_returnsCreated() throws Exception {
        EbookDto dto = createEbookDto();
        when(gutenbergService.importBook(eq(1342), eq(ownerId))).thenReturn(dto);

        mockMvc.perform(post("/api/library/gutenberg/1342/import")
                        .with(user(ownerUser)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.success").value(true));
    }

    @Test
    void importGutenberg_asMember_returns403() throws Exception {
        mockMvc.perform(post("/api/library/gutenberg/1342/import")
                        .with(user(memberUser)))
                .andExpect(status().isForbidden());
    }

    @Test
    void importGutenberg_unauthenticated_returns401() throws Exception {
        mockMvc.perform(post("/api/library/gutenberg/1342/import"))
                .andExpect(status().isUnauthorized());
    }

    // ── Kiwix Process Management Endpoints ─────────────────────────────────

    @Test
    void startKiwix_asOwner_returnsOk() throws Exception {
        mockMvc.perform(post("/api/library/kiwix/start")
                        .with(user(ownerUser)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true));

        verify(kiwixProcessService).start();
    }

    @Test
    void startKiwix_asMember_returns403() throws Exception {
        mockMvc.perform(post("/api/library/kiwix/start")
                        .with(user(memberUser)))
                .andExpect(status().isForbidden());
    }

    @Test
    void startKiwix_unauthenticated_returns401() throws Exception {
        mockMvc.perform(post("/api/library/kiwix/start"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void stopKiwix_asOwner_returnsOk() throws Exception {
        mockMvc.perform(post("/api/library/kiwix/stop")
                        .with(user(ownerUser)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true));

        verify(kiwixProcessService).stop();
    }

    @Test
    void stopKiwix_asMember_returns403() throws Exception {
        mockMvc.perform(post("/api/library/kiwix/stop")
                        .with(user(memberUser)))
                .andExpect(status().isForbidden());
    }

    @Test
    void installKiwix_asOwner_returnsOk() throws Exception {
        mockMvc.perform(post("/api/library/kiwix/install")
                        .with(user(ownerUser)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true));

        verify(kiwixProcessService).installKiwix();
    }

    @Test
    void installKiwix_asMember_returns403() throws Exception {
        mockMvc.perform(post("/api/library/kiwix/install")
                        .with(user(memberUser)))
                .andExpect(status().isForbidden());
    }

    @Test
    void installKiwix_unauthenticated_returns401() throws Exception {
        mockMvc.perform(post("/api/library/kiwix/install"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void browseKiwixCatalog_authenticated_returnsOk() throws Exception {
        KiwixCatalogSearchResultDto result = new KiwixCatalogSearchResultDto(1, List.of(
                new KiwixCatalogEntryDto("abc-123", "Wikipedia (English)", "English Wikipedia",
                        "eng", "wikipedia_en", "wikipedia", "wikipedia;english",
                        6000000, 500000, 95000000000L,
                        "https://download.kiwix.org/wikipedia_en.zim",
                        "https://library.kiwix.org/catalog/v2/illustration/abc-123/?size=48")));
        when(kiwixCatalogService.browse(null, null, 20, 0)).thenReturn(result);

        mockMvc.perform(get("/api/library/kiwix/catalog/browse")
                        .with(user(memberUser)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.totalCount").value(1))
                .andExpect(jsonPath("$.data.entries[0].title").value("Wikipedia (English)"));
    }

    @Test
    void browseKiwixCatalog_unauthenticated_returns401() throws Exception {
        mockMvc.perform(get("/api/library/kiwix/catalog/browse"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void searchKiwixCatalog_authenticated_returnsOk() throws Exception {
        KiwixCatalogSearchResultDto result = new KiwixCatalogSearchResultDto(1, List.of(
                new KiwixCatalogEntryDto("abc-123", "Wikipedia", "English Wikipedia",
                        "eng", "wikipedia_en", "wikipedia", null,
                        6000000, 500000, 95000000000L, null, null)));
        when(kiwixCatalogService.search("wikipedia", null, 20)).thenReturn(result);

        mockMvc.perform(get("/api/library/kiwix/catalog/search")
                        .param("q", "wikipedia")
                        .with(user(memberUser)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.totalCount").value(1))
                .andExpect(jsonPath("$.data.entries[0].title").value("Wikipedia"));
    }

    @Test
    void searchKiwixCatalog_unauthenticated_returns401() throws Exception {
        mockMvc.perform(get("/api/library/kiwix/catalog/search")
                        .param("q", "test"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void downloadFromCatalog_asOwner_returnsOk() throws Exception {
        KiwixCatalogDownloadRequest request = new KiwixCatalogDownloadRequest(
                "https://download.kiwix.org/test.zim", "test.zim",
                "Test Wikipedia", "wikipedia", "eng", 1024000);
        when(kiwixDownloadService.startDownload(any(KiwixCatalogDownloadRequest.class), eq(ownerId)))
                .thenReturn("download-123");

        mockMvc.perform(post("/api/library/kiwix/catalog/download")
                        .contentType("application/json")
                        .content(objectMapper.writeValueAsString(request))
                        .with(user(ownerUser)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data").value("download-123"));
    }

    @Test
    void downloadFromCatalog_asMember_returns403() throws Exception {
        KiwixCatalogDownloadRequest request = new KiwixCatalogDownloadRequest(
                "https://download.kiwix.org/test.zim", "test.zim",
                "Test", "wikipedia", "eng", 1024);

        mockMvc.perform(post("/api/library/kiwix/catalog/download")
                        .contentType("application/json")
                        .content(objectMapper.writeValueAsString(request))
                        .with(user(memberUser)))
                .andExpect(status().isForbidden());
    }

    @Test
    void listKiwixDownloads_authenticated_returnsOk() throws Exception {
        when(kiwixDownloadService.getAllDownloads()).thenReturn(List.of(
                new KiwixDownloadStatusDto("dl-1", "test.zim", 1024000, 512000,
                        50.0, KiwixDownloadState.DOWNLOADING, null)));

        mockMvc.perform(get("/api/library/kiwix/downloads")
                        .with(user(memberUser)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data[0].id").value("dl-1"))
                .andExpect(jsonPath("$.data[0].percentComplete").value(50.0));
    }

    @Test
    void listKiwixDownloads_unauthenticated_returns401() throws Exception {
        mockMvc.perform(get("/api/library/kiwix/downloads"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void getKiwixDownloadProgress_found_returnsOk() throws Exception {
        when(kiwixDownloadService.getProgress("dl-1")).thenReturn(java.util.Optional.of(
                new KiwixDownloadStatusDto("dl-1", "test.zim", 1024, 1024,
                        100.0, KiwixDownloadState.COMPLETE, null)));

        mockMvc.perform(get("/api/library/kiwix/downloads/dl-1")
                        .with(user(memberUser)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.status").value("COMPLETE"));
    }

    @Test
    void getKiwixDownloadProgress_notFound_returns404() throws Exception {
        when(kiwixDownloadService.getProgress("nonexistent")).thenReturn(java.util.Optional.empty());

        mockMvc.perform(get("/api/library/kiwix/downloads/nonexistent")
                        .with(user(memberUser)))
                .andExpect(status().isNotFound());
    }

    // ── Helpers ──────────────────────────────────────────────────────────────

    private ZimFileDto createZimFileDto() {
        return new ZimFileDto(
                UUID.randomUUID(), "wikipedia.zim", "Wikipedia", "Wikipedia offline",
                "en", "reference", 1048576, 100, 50, "2026-01-01",
                "wiki-en", Instant.now(), ownerId);
    }

    private EbookDto createEbookDto() {
        return new EbookDto(
                UUID.randomUUID(), "Test Book", "Test Author", "A test book",
                null, null, null, "en", EbookFormat.EPUB, 5000,
                null, 0, false, Instant.now(), ownerId);
    }
}
