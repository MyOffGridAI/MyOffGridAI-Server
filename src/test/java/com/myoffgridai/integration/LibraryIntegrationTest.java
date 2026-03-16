package com.myoffgridai.integration;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.myoffgridai.auth.model.Role;
import com.myoffgridai.auth.model.User;
import com.myoffgridai.auth.repository.UserRepository;
import com.myoffgridai.auth.service.JwtService;
import com.myoffgridai.library.model.Ebook;
import com.myoffgridai.library.model.EbookFormat;
import com.myoffgridai.library.model.ZimFile;
import com.myoffgridai.library.repository.EbookRepository;
import com.myoffgridai.library.repository.ZimFileRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.web.servlet.MockMvc;

import java.nio.file.Files;
import java.nio.file.Path;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Integration tests for the Library system (ZIM files and eBooks).
 *
 * <p>Tests the full stack from HTTP request through controller, service,
 * and repository layers against a real Testcontainers PostgreSQL database.</p>
 */
class LibraryIntegrationTest extends BaseIntegrationTest {

    @Autowired private MockMvc mockMvc;
    @Autowired private ObjectMapper objectMapper;
    @Autowired private UserRepository userRepository;
    @Autowired private ZimFileRepository zimFileRepository;
    @Autowired private EbookRepository ebookRepository;
    @Autowired private JwtService jwtService;
    @Autowired private PasswordEncoder passwordEncoder;

    private String ownerToken;
    private String memberToken;

    @BeforeEach
    void setUp() {
        ebookRepository.deleteAll();
        zimFileRepository.deleteAll();

        User owner = new User();
        owner.setUsername("libraryowner");
        owner.setDisplayName("Library Owner");
        owner.setEmail("libraryowner@test.com");
        owner.setPasswordHash(passwordEncoder.encode("pass"));
        owner.setRole(Role.ROLE_OWNER);
        owner = userRepository.save(owner);
        ownerToken = jwtService.generateAccessToken(owner);

        User member = new User();
        member.setUsername("librarymember");
        member.setDisplayName("Library Member");
        member.setEmail("librarymember@test.com");
        member.setPasswordHash(passwordEncoder.encode("pass"));
        member.setRole(Role.ROLE_MEMBER);
        member = userRepository.save(member);
        memberToken = jwtService.generateAccessToken(member);
    }

    @Test
    void listZimFiles_emptyLibrary_returnsEmptyList() throws Exception {
        mockMvc.perform(get("/api/library/zim")
                        .header("Authorization", "Bearer " + ownerToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data").isArray())
                .andExpect(jsonPath("$.data").isEmpty());
    }

    @Test
    void listEbooks_emptyLibrary_returnsEmptyList() throws Exception {
        mockMvc.perform(get("/api/library/ebooks")
                        .header("Authorization", "Bearer " + ownerToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data").isArray());
    }

    @Test
    void kiwixStatus_returnsStatus() throws Exception {
        mockMvc.perform(get("/api/library/kiwix/status")
                        .header("Authorization", "Bearer " + memberToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.url").exists());
    }

    @Test
    void kiwixUrl_returnsUrl() throws Exception {
        mockMvc.perform(get("/api/library/kiwix/url")
                        .header("Authorization", "Bearer " + memberToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data").isString());
    }

    @Test
    void gutenbergSearch_unauthenticated_returns401() throws Exception {
        mockMvc.perform(get("/api/library/gutenberg/search")
                        .param("query", "test"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void uploadZim_asMember_returns403() throws Exception {
        MockMultipartFile file = new MockMultipartFile(
                "file", "test.zim", "application/octet-stream", "data".getBytes());

        mockMvc.perform(multipart("/api/library/zim")
                        .file(file)
                        .param("displayName", "Test")
                        .header("Authorization", "Bearer " + memberToken))
                .andExpect(status().isForbidden());
    }

    @Test
    void uploadEbook_asMember_returns403() throws Exception {
        MockMultipartFile file = new MockMultipartFile(
                "file", "book.epub", "application/epub+zip", "data".getBytes());

        mockMvc.perform(multipart("/api/library/ebooks")
                        .file(file)
                        .param("title", "Book")
                        .header("Authorization", "Bearer " + memberToken))
                .andExpect(status().isForbidden());
    }

    @Test
    void importGutenberg_asMember_returns403() throws Exception {
        mockMvc.perform(post("/api/library/gutenberg/1342/import")
                        .header("Authorization", "Bearer " + memberToken))
                .andExpect(status().isForbidden());
    }

    @Test
    void zimEntitiesPersist() {
        ZimFile zf = new ZimFile();
        zf.setFilename("test-persist.zim");
        zf.setDisplayName("Test Persist");
        zf.setFilePath("/tmp/test-persist.zim");
        zf.setFileSizeBytes(1024);

        ZimFile saved = zimFileRepository.save(zf);
        assertThat(saved.getId()).isNotNull();
        assertThat(saved.getUploadedAt()).isNotNull();
        assertThat(zimFileRepository.existsByFilename("test-persist.zim")).isTrue();
    }

    @Test
    void ebookEntitiesPersist() {
        Ebook ebook = new Ebook();
        ebook.setTitle("Persisted Book");
        ebook.setFormat(EbookFormat.EPUB);
        ebook.setFilePath("/tmp/test.epub");
        ebook.setFileSizeBytes(2048);

        Ebook saved = ebookRepository.save(ebook);
        assertThat(saved.getId()).isNotNull();
        assertThat(saved.getUploadedAt()).isNotNull();
    }

    @Test
    void ebookSearchByGutenbergId() {
        Ebook ebook = new Ebook();
        ebook.setTitle("Gutenberg Book");
        ebook.setFormat(EbookFormat.TXT);
        ebook.setFilePath("/tmp/gut.txt");
        ebook.setFileSizeBytes(512);
        ebook.setGutenbergId("12345");
        ebookRepository.save(ebook);

        assertThat(ebookRepository.existsByGutenbergId("12345")).isTrue();
        assertThat(ebookRepository.existsByGutenbergId("99999")).isFalse();
        assertThat(ebookRepository.findByGutenbergId("12345")).isPresent();
    }
}
