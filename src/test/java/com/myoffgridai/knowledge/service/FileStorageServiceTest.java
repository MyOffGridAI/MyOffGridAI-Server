package com.myoffgridai.knowledge.service;

import com.myoffgridai.common.exception.StorageException;
import com.myoffgridai.system.model.SystemConfig;
import com.myoffgridai.system.service.SystemConfigService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.io.TempDir;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockMultipartFile;

import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.UUID;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class FileStorageServiceTest {

    @Mock private SystemConfigService systemConfigService;

    private FileStorageService fileStorageService;
    private UUID userId;

    @TempDir
    Path tempDir;

    @BeforeEach
    void setUp() {
        SystemConfig config = new SystemConfig();
        config.setKnowledgeStoragePath(tempDir.toAbsolutePath().toString());
        lenient().when(systemConfigService.getConfig()).thenReturn(config);

        fileStorageService = new FileStorageService(systemConfigService);
        userId = UUID.randomUUID();
    }

    @Test
    void store_savesFileSuccessfully() throws Exception {
        MockMultipartFile file = new MockMultipartFile(
                "file", "test.txt", "text/plain", "Hello World".getBytes());

        String storagePath = fileStorageService.store(userId, file, "test.txt");

        assertThat(storagePath).isNotEmpty();
        assertThat(Files.exists(Path.of(storagePath))).isTrue();
        assertThat(Files.readString(Path.of(storagePath))).isEqualTo("Hello World");
    }

    @Test
    void store_sanitizesFilename() throws Exception {
        MockMultipartFile file = new MockMultipartFile(
                "file", "my file (1).txt", "text/plain", "content".getBytes());

        String storagePath = fileStorageService.store(userId, file, "my file (1).txt");

        assertThat(storagePath).doesNotContain(" ").doesNotContain("(");
    }

    @Test
    void delete_removesFile() throws Exception {
        MockMultipartFile file = new MockMultipartFile(
                "file", "delete-me.txt", "text/plain", "to delete".getBytes());

        String storagePath = fileStorageService.store(userId, file, "delete-me.txt");
        assertThat(Files.exists(Path.of(storagePath))).isTrue();

        fileStorageService.delete(storagePath);
        assertThat(Files.exists(Path.of(storagePath))).isFalse();
    }

    @Test
    void delete_nonExistentFile_doesNotThrow() {
        assertThatCode(() -> fileStorageService.delete("/nonexistent/path/file.txt"))
                .doesNotThrowAnyException();
    }

    @Test
    void deleteAllForUser_removesUserDirectory() throws Exception {
        MockMultipartFile file1 = new MockMultipartFile(
                "file", "f1.txt", "text/plain", "one".getBytes());
        MockMultipartFile file2 = new MockMultipartFile(
                "file", "f2.txt", "text/plain", "two".getBytes());

        fileStorageService.store(userId, file1, "f1.txt");
        fileStorageService.store(userId, file2, "f2.txt");

        Path userDir = tempDir.resolve(userId.toString());
        assertThat(Files.exists(userDir)).isTrue();

        fileStorageService.deleteAllForUser(userId);
        assertThat(Files.exists(userDir)).isFalse();
    }

    @Test
    void deleteAllForUser_nonExistentUser_doesNotThrow() {
        assertThatCode(() -> fileStorageService.deleteAllForUser(UUID.randomUUID()))
                .doesNotThrowAnyException();
    }

    @Test
    void getInputStream_returnsFileContent() throws Exception {
        MockMultipartFile file = new MockMultipartFile(
                "file", "read.txt", "text/plain", "readable content".getBytes());

        String storagePath = fileStorageService.store(userId, file, "read.txt");

        try (InputStream is = fileStorageService.getInputStream(storagePath)) {
            String content = new String(is.readAllBytes());
            assertThat(content).isEqualTo("readable content");
        }
    }

    @Test
    void getInputStream_nonExistentFile_throwsStorageException() {
        assertThatThrownBy(() -> fileStorageService.getInputStream("/nonexistent/file.txt"))
                .isInstanceOf(StorageException.class);
    }
}
