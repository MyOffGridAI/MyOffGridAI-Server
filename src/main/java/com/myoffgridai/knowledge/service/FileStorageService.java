package com.myoffgridai.knowledge.service;

import com.myoffgridai.common.exception.StorageException;
import com.myoffgridai.system.service.SystemConfigService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.UUID;

/**
 * Manages local filesystem storage for uploaded knowledge documents.
 *
 * <p>Files are stored under {@code {base}/{userId}/{uuid}-{filename}} to prevent
 * collisions and support per-user isolation.</p>
 */
@Service
public class FileStorageService {

    private static final Logger log = LoggerFactory.getLogger(FileStorageService.class);

    private final SystemConfigService systemConfigService;

    /**
     * Constructs the file storage service.
     *
     * @param systemConfigService the system config service for resolving the storage path
     */
    public FileStorageService(SystemConfigService systemConfigService) {
        this.systemConfigService = systemConfigService;
    }

    /**
     * Stores an uploaded file to the local filesystem under the user's directory.
     *
     * @param userId   the owning user's ID
     * @param file     the uploaded multipart file
     * @param filename the original filename
     * @return the absolute storage path
     * @throws StorageException if the file cannot be written
     */
    public String store(UUID userId, MultipartFile file, String filename) {
        try {
            Path userDir = Paths.get(getStorageBasePath(), userId.toString());
            Files.createDirectories(userDir);

            String safeFilename = UUID.randomUUID() + "-" + sanitizeFilename(filename);
            Path target = userDir.resolve(safeFilename);

            Files.copy(file.getInputStream(), target, StandardCopyOption.REPLACE_EXISTING);
            log.info("Stored file: {} ({} bytes)", target, file.getSize());
            return target.toAbsolutePath().toString();
        } catch (IOException e) {
            throw new StorageException("Failed to store file: " + filename, e);
        }
    }

    /**
     * Stores raw bytes to the local filesystem under the user's directory.
     *
     * <p>Used for editor-created documents that don't originate from a
     * {@link MultipartFile} upload.</p>
     *
     * @param userId   the owning user's ID
     * @param bytes    the file content as a byte array
     * @param filename the filename to use (will be sanitized)
     * @return the absolute storage path
     * @throws StorageException if the file cannot be written
     */
    public String storeBytes(UUID userId, byte[] bytes, String filename) {
        try {
            Path userDir = Paths.get(getStorageBasePath(), userId.toString());
            Files.createDirectories(userDir);

            String safeFilename = UUID.randomUUID() + "-" + sanitizeFilename(filename);
            Path target = userDir.resolve(safeFilename);

            Files.write(target, bytes);
            log.info("Stored bytes: {} ({} bytes)", target, bytes.length);
            return target.toAbsolutePath().toString();
        } catch (IOException e) {
            throw new StorageException("Failed to store file: " + filename, e);
        }
    }

    /**
     * Deletes a file from the local filesystem.
     *
     * @param storagePath the absolute path of the file to delete
     */
    public void delete(String storagePath) {
        try {
            Path path = Paths.get(storagePath);
            if (Files.exists(path)) {
                Files.delete(path);
                log.info("Deleted file: {}", storagePath);
            }
        } catch (IOException e) {
            log.warn("Failed to delete file: {}", storagePath, e);
        }
    }

    /**
     * Deletes all stored files for a given user.
     *
     * @param userId the user's ID
     */
    public void deleteAllForUser(UUID userId) {
        Path userDir = Paths.get(getStorageBasePath(), userId.toString());
        if (!Files.exists(userDir)) {
            return;
        }
        try (var stream = Files.walk(userDir)) {
            stream.sorted(java.util.Comparator.reverseOrder())
                    .forEach(path -> {
                        try {
                            Files.delete(path);
                        } catch (IOException e) {
                            log.warn("Failed to delete: {}", path, e);
                        }
                    });
            log.info("Deleted all files for user: {}", userId);
        } catch (IOException e) {
            log.warn("Failed to walk user directory: {}", userDir, e);
        }
    }

    /**
     * Opens an input stream for a stored file.
     *
     * @param storagePath the absolute path of the file
     * @return an input stream to the file content
     * @throws StorageException if the file cannot be read
     */
    public InputStream getInputStream(String storagePath) {
        try {
            return Files.newInputStream(Paths.get(storagePath));
        } catch (IOException e) {
            throw new StorageException("Failed to read file: " + storagePath, e);
        }
    }

    private String getStorageBasePath() {
        return systemConfigService.getConfig().getKnowledgeStoragePath();
    }

    private String sanitizeFilename(String filename) {
        if (filename == null || filename.isBlank()) {
            return "unknown";
        }
        return filename.replaceAll("[^a-zA-Z0-9._-]", "_");
    }
}
