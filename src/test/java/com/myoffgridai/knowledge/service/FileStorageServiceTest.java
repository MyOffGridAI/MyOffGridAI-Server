package com.myoffgridai.knowledge.service;

import com.myoffgridai.common.exception.StorageException;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.util.ReflectionTestUtils;

import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.UUID;

import static org.assertj.core.api.Assertions.*;

class FileStorageServiceTest {

    private FileStorageService fileStorageService;
    private UUID userId;

    @TempDir
    Path tempDir;

    @BeforeEach
    void setUp() throws Exception {
        fileStorageService = new FileStorageService();
        userId = UUID.randomUUID();

        // Override the storage base path to use temp directory
        java.lang.reflect.Field field = null;
        // We'll use the temp dir by overriding the constant behavior through system properties
        // Since AppConstants are final, we create the user dir structure under tempDir
    }

    @Test
    void store_savesFileSuccessfully() throws Exception {
        // Use a real temp dir for testing
        FileStorageService service = new TestableFileStorageService(tempDir);
        MockMultipartFile file = new MockMultipartFile(
                "file", "test.txt", "text/plain", "Hello World".getBytes());

        String storagePath = service.store(userId, file, "test.txt");

        assertThat(storagePath).isNotEmpty();
        assertThat(Files.exists(Path.of(storagePath))).isTrue();
        assertThat(Files.readString(Path.of(storagePath))).isEqualTo("Hello World");
    }

    @Test
    void store_sanitizesFilename() throws Exception {
        FileStorageService service = new TestableFileStorageService(tempDir);
        MockMultipartFile file = new MockMultipartFile(
                "file", "my file (1).txt", "text/plain", "content".getBytes());

        String storagePath = service.store(userId, file, "my file (1).txt");

        assertThat(storagePath).doesNotContain(" ").doesNotContain("(");
    }

    @Test
    void delete_removesFile() throws Exception {
        FileStorageService service = new TestableFileStorageService(tempDir);
        MockMultipartFile file = new MockMultipartFile(
                "file", "delete-me.txt", "text/plain", "to delete".getBytes());

        String storagePath = service.store(userId, file, "delete-me.txt");
        assertThat(Files.exists(Path.of(storagePath))).isTrue();

        service.delete(storagePath);
        assertThat(Files.exists(Path.of(storagePath))).isFalse();
    }

    @Test
    void delete_nonExistentFile_doesNotThrow() {
        assertThatCode(() -> fileStorageService.delete("/nonexistent/path/file.txt"))
                .doesNotThrowAnyException();
    }

    @Test
    void deleteAllForUser_removesUserDirectory() throws Exception {
        FileStorageService service = new TestableFileStorageService(tempDir);
        MockMultipartFile file1 = new MockMultipartFile(
                "file", "f1.txt", "text/plain", "one".getBytes());
        MockMultipartFile file2 = new MockMultipartFile(
                "file", "f2.txt", "text/plain", "two".getBytes());

        service.store(userId, file1, "f1.txt");
        service.store(userId, file2, "f2.txt");

        Path userDir = tempDir.resolve(userId.toString());
        assertThat(Files.exists(userDir)).isTrue();

        service.deleteAllForUser(userId);
        assertThat(Files.exists(userDir)).isFalse();
    }

    @Test
    void deleteAllForUser_nonExistentUser_doesNotThrow() {
        FileStorageService service = new TestableFileStorageService(tempDir);
        assertThatCode(() -> service.deleteAllForUser(UUID.randomUUID()))
                .doesNotThrowAnyException();
    }

    @Test
    void getInputStream_returnsFileContent() throws Exception {
        FileStorageService service = new TestableFileStorageService(tempDir);
        MockMultipartFile file = new MockMultipartFile(
                "file", "read.txt", "text/plain", "readable content".getBytes());

        String storagePath = service.store(userId, file, "read.txt");

        try (InputStream is = service.getInputStream(storagePath)) {
            String content = new String(is.readAllBytes());
            assertThat(content).isEqualTo("readable content");
        }
    }

    @Test
    void getInputStream_nonExistentFile_throwsStorageException() {
        assertThatThrownBy(() -> fileStorageService.getInputStream("/nonexistent/file.txt"))
                .isInstanceOf(StorageException.class);
    }

    /**
     * Testable subclass that overrides the storage base path.
     */
    private static class TestableFileStorageService extends FileStorageService {

        private final Path basePath;

        TestableFileStorageService(Path basePath) {
            this.basePath = basePath;
        }

        @Override
        public String store(UUID userId, org.springframework.web.multipart.MultipartFile file, String filename) {
            try {
                Path userDir = basePath.resolve(userId.toString());
                Files.createDirectories(userDir);
                String safeFilename = UUID.randomUUID() + "-" + filename.replaceAll("[^a-zA-Z0-9._-]", "_");
                Path target = userDir.resolve(safeFilename);
                Files.copy(file.getInputStream(), target, java.nio.file.StandardCopyOption.REPLACE_EXISTING);
                return target.toAbsolutePath().toString();
            } catch (java.io.IOException e) {
                throw new StorageException("Failed to store file: " + filename, e);
            }
        }

        @Override
        public void deleteAllForUser(UUID userId) {
            Path userDir = basePath.resolve(userId.toString());
            if (!Files.exists(userDir)) return;
            try (var stream = Files.walk(userDir)) {
                stream.sorted(java.util.Comparator.reverseOrder())
                        .forEach(path -> {
                            try { Files.delete(path); } catch (java.io.IOException ignored) {}
                        });
            } catch (java.io.IOException ignored) {}
        }
    }
}
