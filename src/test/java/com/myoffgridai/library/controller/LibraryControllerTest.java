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
        KiwixStatusDto status = new KiwixStatusDto(true, "http://localhost:8888", 3);
        when(zimFileService.getKiwixStatus()).thenReturn(status);

        mockMvc.perform(get("/api/library/kiwix/status")
                        .with(user(memberUser)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.available").value(true))
                .andExpect(jsonPath("$.data.bookCount").value(3));
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
