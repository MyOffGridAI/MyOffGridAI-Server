package com.myoffgridai.library.service;

import com.myoffgridai.library.model.EbookFormat;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Files;
import java.nio.file.Path;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Unit tests for {@link CalibreConversionService}.
 *
 * <p>These tests verify the service logic without actually running Docker.
 * Docker-dependent behavior is tested via the return values and error handling.</p>
 */
class CalibreConversionServiceTest {

    @TempDir
    Path tempDir;

    private CalibreConversionService calibreConversionService;

    @BeforeEach
    void setUp() {
        calibreConversionService = new CalibreConversionService();
    }

    @Test
    void convertToEpub_alreadyEpub_returnsSamePath() throws Exception {
        Path epubFile = tempDir.resolve("book.epub");
        Files.createFile(epubFile);

        Path result = calibreConversionService.convertToEpub(epubFile, EbookFormat.EPUB);

        assertThat(result).isEqualTo(epubFile);
    }

    @Test
    void convertToEpub_mobiFile_dockerNotAvailable_returnsNull() throws Exception {
        Path mobiFile = tempDir.resolve("book.mobi");
        Files.createFile(mobiFile);

        // Docker exec will fail since no container is running in test
        Path result = calibreConversionService.convertToEpub(mobiFile, EbookFormat.MOBI);

        // In a test environment without Docker, this should return null (conversion fails)
        assertThat(result).isNull();
    }

    @Test
    void convertToEpub_azwFile_dockerNotAvailable_returnsNull() throws Exception {
        Path azwFile = tempDir.resolve("book.azw");
        Files.createFile(azwFile);

        Path result = calibreConversionService.convertToEpub(azwFile, EbookFormat.AZW);

        assertThat(result).isNull();
    }

    @Test
    void isAvailable_noDocker_returnsFalse() {
        // In a CI/test environment, Docker container is not running
        boolean result = calibreConversionService.isAvailable();

        assertThat(result).isFalse();
    }

    @Test
    void convertToEpub_pdfFile_dockerNotAvailable_returnsNull() throws Exception {
        Path pdfFile = tempDir.resolve("document.pdf");
        Files.createFile(pdfFile);

        Path result = calibreConversionService.convertToEpub(pdfFile, EbookFormat.PDF);

        assertThat(result).isNull();
    }
}
