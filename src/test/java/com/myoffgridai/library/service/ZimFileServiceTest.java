package com.myoffgridai.library.service;

import com.myoffgridai.library.config.KiwixProperties;
import com.myoffgridai.library.config.LibraryProperties;
import com.myoffgridai.library.dto.KiwixInstallationStatus;
import com.myoffgridai.library.dto.KiwixStatusDto;
import com.myoffgridai.library.dto.ZimFileDto;
import com.myoffgridai.library.model.ZimFile;
import com.myoffgridai.library.repository.ZimFileRepository;
import jakarta.persistence.EntityNotFoundException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.io.TempDir;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.nio.file.Path;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;
import static org.mockito.Mockito.lenient;

/**
 * Unit tests for {@link ZimFileService}.
 */
@ExtendWith(MockitoExtension.class)
class ZimFileServiceTest {

    @Mock private ZimFileRepository zimFileRepository;
    @Mock private LibraryProperties libraryProperties;
    @Mock private KiwixProperties kiwixProperties;
    @Mock private KiwixProcessService kiwixProcessService;
    @Mock private WebClient.Builder webClientBuilder;
    @Mock private WebClient webClient;

    @TempDir
    Path tempDir;

    private ZimFileService zimFileService;

    @BeforeEach
    void setUp() {
        when(webClientBuilder.build()).thenReturn(webClient);
        zimFileService = new ZimFileService(zimFileRepository, libraryProperties,
                kiwixProperties, kiwixProcessService, webClientBuilder);
    }

    @Test
    void upload_validZimFile_savesAndReturnsDto() throws IOException {
        MultipartFile file = mockMultipartFile("wikipedia.zim", 1024);
        when(libraryProperties.getZimDirectory()).thenReturn(tempDir.toString());
        when(libraryProperties.getMaxUploadSizeMb()).thenReturn(2048);
        when(zimFileRepository.existsByFilename("wikipedia.zim")).thenReturn(false);
        when(zimFileRepository.save(any(ZimFile.class))).thenAnswer(inv -> {
            ZimFile zf = inv.getArgument(0);
            zf.setId(UUID.randomUUID());
            zf.setUploadedAt(Instant.now());
            return zf;
        });

        ZimFileDto result = zimFileService.upload(file, "Wikipedia", "reference", UUID.randomUUID());

        assertThat(result).isNotNull();
        assertThat(result.filename()).isEqualTo("wikipedia.zim");
        assertThat(result.displayName()).isEqualTo("Wikipedia");
        assertThat(result.category()).isEqualTo("reference");
        verify(zimFileRepository).save(any(ZimFile.class));
    }

    @Test
    void upload_nonZimExtension_throwsIllegalArgument() throws IOException {
        MultipartFile file = mockMultipartFile("document.pdf", 1024);

        assertThatThrownBy(() -> zimFileService.upload(file, "Doc", null, UUID.randomUUID()))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Only .zim files are supported");
    }

    @Test
    void upload_duplicateFilename_throwsIllegalArgument() throws IOException {
        MultipartFile file = mockMultipartFile("wikipedia.zim", 1024);
        when(zimFileRepository.existsByFilename("wikipedia.zim")).thenReturn(true);

        assertThatThrownBy(() -> zimFileService.upload(file, "Wiki", null, UUID.randomUUID()))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("already exists");
    }

    @Test
    void upload_exceedsMaxSize_throwsIllegalArgument() throws IOException {
        MultipartFile file = mockMultipartFile("huge.zim", 3L * 1024 * 1024 * 1024);
        when(zimFileRepository.existsByFilename("huge.zim")).thenReturn(false);
        when(libraryProperties.getMaxUploadSizeMb()).thenReturn(2048);

        assertThatThrownBy(() -> zimFileService.upload(file, "Huge", null, UUID.randomUUID()))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("exceeds the maximum");
    }

    @Test
    void listAll_returnsAllZimFiles() {
        ZimFile z1 = createZimFile("alpha.zim", "Alpha");
        ZimFile z2 = createZimFile("beta.zim", "Beta");
        when(zimFileRepository.findAllByOrderByDisplayNameAsc()).thenReturn(List.of(z1, z2));

        List<ZimFileDto> result = zimFileService.listAll();

        assertThat(result).hasSize(2);
        assertThat(result.get(0).displayName()).isEqualTo("Alpha");
        assertThat(result.get(1).displayName()).isEqualTo("Beta");
    }

    @Test
    void delete_existingFile_deletesFromDbAndDisk() throws IOException {
        UUID id = UUID.randomUUID();
        Path filePath = tempDir.resolve("test.zim");
        java.nio.file.Files.createFile(filePath);

        ZimFile zimFile = createZimFile("test.zim", "Test");
        zimFile.setId(id);
        zimFile.setFilePath(filePath.toString());
        when(zimFileRepository.findById(id)).thenReturn(Optional.of(zimFile));

        zimFileService.delete(id);

        verify(zimFileRepository).delete(zimFile);
        assertThat(java.nio.file.Files.exists(filePath)).isFalse();
    }

    @Test
    void delete_nonExistentFile_throwsEntityNotFound() {
        UUID id = UUID.randomUUID();
        when(zimFileRepository.findById(id)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> zimFileService.delete(id))
                .isInstanceOf(EntityNotFoundException.class);
    }

    @Test
    void getKiwixServeUrl_returnsConfiguredUrl() {
        when(libraryProperties.getKiwixUrl()).thenReturn("http://localhost:8888");

        assertThat(zimFileService.getKiwixServeUrl()).isEqualTo("http://localhost:8888");
    }

    @Test
    @SuppressWarnings("unchecked")
    void getKiwixStatus_serverAvailable_returnsAvailable() {
        when(libraryProperties.getKiwixUrl()).thenReturn("http://localhost:8888");
        when(zimFileRepository.count()).thenReturn(3L);
        when(kiwixProperties.isManageProcess()).thenReturn(true);
        when(kiwixProcessService.getInstallationStatus()).thenReturn(KiwixInstallationStatus.INSTALLED);
        when(kiwixProcessService.getInstallationError()).thenReturn(null);

        WebClient.RequestHeadersUriSpec requestSpec = mock(WebClient.RequestHeadersUriSpec.class);
        WebClient.RequestHeadersSpec headersSpec = mock(WebClient.RequestHeadersSpec.class);
        WebClient.ResponseSpec responseSpec = mock(WebClient.ResponseSpec.class);

        when(webClient.get()).thenReturn(requestSpec);
        when(requestSpec.uri(anyString())).thenReturn(headersSpec);
        when(headersSpec.retrieve()).thenReturn(responseSpec);
        when(responseSpec.toBodilessEntity()).thenReturn(Mono.empty());

        KiwixStatusDto result = zimFileService.getKiwixStatus();

        assertThat(result.available()).isTrue();
        assertThat(result.bookCount()).isEqualTo(3);
        assertThat(result.processManaged()).isTrue();
    }

    @Test
    void getKiwixStatus_serverUnavailable_returnsUnavailable() {
        when(libraryProperties.getKiwixUrl()).thenReturn("http://localhost:8888");
        when(zimFileRepository.count()).thenReturn(2L);
        when(kiwixProperties.isManageProcess()).thenReturn(false);
        when(kiwixProcessService.getInstallationStatus()).thenReturn(KiwixInstallationStatus.NOT_INSTALLED);
        when(kiwixProcessService.getInstallationError()).thenReturn(null);

        when(webClient.get()).thenThrow(new RuntimeException("Connection refused"));

        KiwixStatusDto result = zimFileService.getKiwixStatus();

        assertThat(result.available()).isFalse();
        assertThat(result.bookCount()).isEqualTo(2);
        assertThat(result.processManaged()).isFalse();
    }

    private MultipartFile mockMultipartFile(String filename, long size) throws IOException {
        MultipartFile file = mock(MultipartFile.class);
        lenient().when(file.getOriginalFilename()).thenReturn(filename);
        lenient().when(file.getSize()).thenReturn(size);
        lenient().when(file.getInputStream()).thenReturn(new ByteArrayInputStream("test content".getBytes()));
        return file;
    }

    private ZimFile createZimFile(String filename, String displayName) {
        ZimFile zf = new ZimFile();
        zf.setId(UUID.randomUUID());
        zf.setFilename(filename);
        zf.setDisplayName(displayName);
        zf.setFileSizeBytes(1024);
        zf.setFilePath("/tmp/" + filename);
        zf.setUploadedAt(Instant.now());
        zf.setUploadedBy(UUID.randomUUID());
        return zf;
    }
}
