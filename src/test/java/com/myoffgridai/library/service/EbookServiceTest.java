package com.myoffgridai.library.service;

import com.myoffgridai.library.config.LibraryProperties;
import com.myoffgridai.library.dto.EbookDto;
import com.myoffgridai.library.model.Ebook;
import com.myoffgridai.library.model.EbookFormat;
import com.myoffgridai.library.repository.EbookRepository;
import jakarta.persistence.EntityNotFoundException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.io.TempDir;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.web.multipart.MultipartFile;

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
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;
import static org.mockito.Mockito.lenient;

/**
 * Unit tests for {@link EbookService}.
 */
@ExtendWith(MockitoExtension.class)
class EbookServiceTest {

    @Mock private EbookRepository ebookRepository;
    @Mock private LibraryProperties libraryProperties;
    @Mock private CalibreConversionService calibreConversionService;

    @TempDir
    Path tempDir;

    private EbookService ebookService;

    @BeforeEach
    void setUp() {
        ebookService = new EbookService(ebookRepository, libraryProperties, calibreConversionService);
    }

    @Test
    void upload_validEpub_savesAndReturnsDto() throws IOException {
        MultipartFile file = mockMultipartFile("book.epub", 5000);
        when(libraryProperties.getEbookDirectory()).thenReturn(tempDir.toString());
        when(ebookRepository.save(any(Ebook.class))).thenAnswer(inv -> {
            Ebook e = inv.getArgument(0);
            e.setId(UUID.randomUUID());
            e.setUploadedAt(Instant.now());
            return e;
        });

        EbookDto result = ebookService.upload(file, "My Book", "Author", UUID.randomUUID());

        assertThat(result).isNotNull();
        assertThat(result.title()).isEqualTo("My Book");
        assertThat(result.author()).isEqualTo("Author");
        assertThat(result.format()).isEqualTo(EbookFormat.EPUB);
        verify(ebookRepository).save(any(Ebook.class));
    }

    @Test
    void upload_validPdf_detectsFormat() throws IOException {
        MultipartFile file = mockMultipartFile("manual.pdf", 3000);
        when(libraryProperties.getEbookDirectory()).thenReturn(tempDir.toString());
        when(ebookRepository.save(any(Ebook.class))).thenAnswer(inv -> {
            Ebook e = inv.getArgument(0);
            e.setId(UUID.randomUUID());
            e.setUploadedAt(Instant.now());
            return e;
        });

        EbookDto result = ebookService.upload(file, "Manual", null, UUID.randomUUID());

        assertThat(result.format()).isEqualTo(EbookFormat.PDF);
    }

    @Test
    void upload_mobiFile_triggersConversion() throws IOException {
        MultipartFile file = mockMultipartFile("novel.mobi", 2000);
        when(libraryProperties.getEbookDirectory()).thenReturn(tempDir.toString());
        when(calibreConversionService.isAvailable()).thenReturn(true);
        when(calibreConversionService.convertToEpub(any(Path.class), eq(EbookFormat.MOBI)))
                .thenAnswer(inv -> {
                    Path source = inv.getArgument(0);
                    Path epubPath = source.getParent().resolve(
                            source.getFileName().toString().replace(".mobi", ".epub"));
                    java.nio.file.Files.createFile(epubPath);
                    return epubPath;
                });
        when(ebookRepository.save(any(Ebook.class))).thenAnswer(inv -> {
            Ebook e = inv.getArgument(0);
            e.setId(UUID.randomUUID());
            e.setUploadedAt(Instant.now());
            return e;
        });

        EbookDto result = ebookService.upload(file, "Novel", null, UUID.randomUUID());

        assertThat(result.format()).isEqualTo(EbookFormat.EPUB);
        verify(calibreConversionService).convertToEpub(any(), eq(EbookFormat.MOBI));
    }

    @Test
    void upload_mobiFile_calibreUnavailable_storesOriginal() throws IOException {
        MultipartFile file = mockMultipartFile("novel.mobi", 2000);
        when(libraryProperties.getEbookDirectory()).thenReturn(tempDir.toString());
        when(calibreConversionService.isAvailable()).thenReturn(false);
        when(ebookRepository.save(any(Ebook.class))).thenAnswer(inv -> {
            Ebook e = inv.getArgument(0);
            e.setId(UUID.randomUUID());
            e.setUploadedAt(Instant.now());
            return e;
        });

        EbookDto result = ebookService.upload(file, "Novel", null, UUID.randomUUID());

        assertThat(result.format()).isEqualTo(EbookFormat.MOBI);
        verify(calibreConversionService, never()).convertToEpub(any(), any());
    }

    @Test
    void upload_unsupportedFormat_throwsIllegalArgument() throws IOException {
        MultipartFile file = mockMultipartFile("data.csv", 1000);

        assertThatThrownBy(() -> ebookService.upload(file, "Data", null, UUID.randomUUID()))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Unsupported eBook format");
    }

    @Test
    void list_withSearchAndFormat_returnsPaginated() {
        Ebook ebook = createEbook("Test Book", EbookFormat.EPUB);
        when(ebookRepository.searchByTitleOrAuthor(eq("test"), eq(EbookFormat.EPUB), any(Pageable.class)))
                .thenReturn(new PageImpl<>(List.of(ebook)));

        var result = ebookService.list("test", EbookFormat.EPUB, PageRequest.of(0, 20));

        assertThat(result.getContent()).hasSize(1);
        assertThat(result.getContent().getFirst().title()).isEqualTo("Test Book");
    }

    @Test
    void get_existingBook_returnsDto() {
        UUID id = UUID.randomUUID();
        Ebook ebook = createEbook("My Book", EbookFormat.PDF);
        ebook.setId(id);
        when(ebookRepository.findById(id)).thenReturn(Optional.of(ebook));

        EbookDto result = ebookService.get(id);

        assertThat(result.title()).isEqualTo("My Book");
        assertThat(result.format()).isEqualTo(EbookFormat.PDF);
    }

    @Test
    void get_nonExistent_throwsEntityNotFound() {
        UUID id = UUID.randomUUID();
        when(ebookRepository.findById(id)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> ebookService.get(id))
                .isInstanceOf(EntityNotFoundException.class);
    }

    @Test
    void delete_existingBook_deletesFromDbAndDisk() throws IOException {
        UUID id = UUID.randomUUID();
        Path filePath = tempDir.resolve("test.epub");
        java.nio.file.Files.createFile(filePath);

        Ebook ebook = createEbook("Test", EbookFormat.EPUB);
        ebook.setId(id);
        ebook.setFilePath(filePath.toString());
        when(ebookRepository.findById(id)).thenReturn(Optional.of(ebook));

        ebookService.delete(id);

        verify(ebookRepository).delete(ebook);
        assertThat(java.nio.file.Files.exists(filePath)).isFalse();
    }

    @Test
    void getForDownload_incrementsCount() {
        UUID id = UUID.randomUUID();
        Ebook ebook = createEbook("Book", EbookFormat.EPUB);
        ebook.setId(id);
        ebook.setDownloadCount(5);
        when(ebookRepository.findById(id)).thenReturn(Optional.of(ebook));
        when(ebookRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        Ebook result = ebookService.getForDownload(id);

        assertThat(result.getDownloadCount()).isEqualTo(6);
        verify(ebookRepository).save(ebook);
    }

    @Test
    void detectFormat_allSupportedFormats() {
        assertThat(ebookService.detectFormat("epub")).isEqualTo(EbookFormat.EPUB);
        assertThat(ebookService.detectFormat("pdf")).isEqualTo(EbookFormat.PDF);
        assertThat(ebookService.detectFormat("mobi")).isEqualTo(EbookFormat.MOBI);
        assertThat(ebookService.detectFormat("azw")).isEqualTo(EbookFormat.AZW);
        assertThat(ebookService.detectFormat("txt")).isEqualTo(EbookFormat.TXT);
        assertThat(ebookService.detectFormat("html")).isEqualTo(EbookFormat.HTML);
        assertThat(ebookService.detectFormat("htm")).isEqualTo(EbookFormat.HTML);
        assertThat(ebookService.detectFormat("xyz")).isNull();
        assertThat(ebookService.detectFormat(null)).isNull();
    }

    private MultipartFile mockMultipartFile(String filename, long size) throws IOException {
        MultipartFile file = mock(MultipartFile.class);
        lenient().when(file.getOriginalFilename()).thenReturn(filename);
        lenient().when(file.getSize()).thenReturn(size);
        lenient().when(file.getInputStream()).thenReturn(new ByteArrayInputStream("content".getBytes()));
        return file;
    }

    private Ebook createEbook(String title, EbookFormat format) {
        Ebook ebook = new Ebook();
        ebook.setId(UUID.randomUUID());
        ebook.setTitle(title);
        ebook.setFormat(format);
        ebook.setFileSizeBytes(1024);
        ebook.setFilePath("/tmp/test.epub");
        ebook.setUploadedAt(Instant.now());
        ebook.setUploadedBy(UUID.randomUUID());
        return ebook;
    }
}
