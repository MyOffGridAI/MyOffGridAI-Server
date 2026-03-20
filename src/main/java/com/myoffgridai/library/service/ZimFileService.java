package com.myoffgridai.library.service;

import com.myoffgridai.library.config.KiwixProperties;
import com.myoffgridai.library.config.LibraryProperties;
import com.myoffgridai.library.dto.KiwixStatusDto;
import com.myoffgridai.library.dto.ZimFileDto;
import com.myoffgridai.library.model.ZimFile;
import com.myoffgridai.library.repository.ZimFileRepository;
import jakarta.persistence.EntityNotFoundException;
import org.apache.commons.io.FilenameUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.reactive.function.client.WebClient;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.time.Duration;
import java.util.List;
import java.util.UUID;

/**
 * Service for managing Kiwix ZIM files in the offline library.
 *
 * <p>Handles upload, validation, listing, deletion, and Kiwix
 * server status checks for ZIM content files.</p>
 */
@Service
public class ZimFileService {

    private static final Logger log = LoggerFactory.getLogger(ZimFileService.class);

    private final ZimFileRepository zimFileRepository;
    private final LibraryProperties libraryProperties;
    private final KiwixProperties kiwixProperties;
    private final KiwixProcessService kiwixProcessService;
    private final WebClient webClient;

    /**
     * Constructs the ZIM file service.
     *
     * @param zimFileRepository    the ZIM file repository
     * @param libraryProperties    the library configuration properties
     * @param kiwixProperties      the kiwix configuration properties
     * @param kiwixProcessService  the kiwix process manager
     * @param webClientBuilder     the WebClient builder for HTTP calls
     */
    public ZimFileService(ZimFileRepository zimFileRepository,
                          LibraryProperties libraryProperties,
                          KiwixProperties kiwixProperties,
                          KiwixProcessService kiwixProcessService,
                          WebClient.Builder webClientBuilder) {
        this.zimFileRepository = zimFileRepository;
        this.libraryProperties = libraryProperties;
        this.kiwixProperties = kiwixProperties;
        this.kiwixProcessService = kiwixProcessService;
        this.webClient = webClientBuilder.build();
    }

    /**
     * Uploads a ZIM file to the library.
     *
     * <p>Validates the file extension (.zim), checks for duplicates,
     * enforces size limits, saves the file to disk, and persists metadata.</p>
     *
     * @param file        the uploaded ZIM file
     * @param displayName the human-readable display name
     * @param category    the content category
     * @param uploadedBy  the ID of the uploading user
     * @return the created ZIM file DTO
     * @throws IllegalArgumentException if validation fails
     * @throws RuntimeException         if file I/O fails
     */
    @Transactional
    public ZimFileDto upload(MultipartFile file, String displayName, String category, UUID uploadedBy) {
        String originalFilename = file.getOriginalFilename();
        String extension = FilenameUtils.getExtension(originalFilename);

        if (!"zim".equalsIgnoreCase(extension)) {
            throw new IllegalArgumentException("Only .zim files are supported, got: ." + extension);
        }

        if (zimFileRepository.existsByFilename(originalFilename)) {
            throw new IllegalArgumentException("A ZIM file with filename '" + originalFilename + "' already exists");
        }

        long maxBytes = (long) libraryProperties.getMaxUploadSizeMb() * 1024 * 1024;
        if (file.getSize() > maxBytes) {
            throw new IllegalArgumentException(
                    String.format("File size (%d MB) exceeds the maximum allowed (%d MB)",
                            file.getSize() / (1024 * 1024), libraryProperties.getMaxUploadSizeMb()));
        }

        Path zimDir = Paths.get(libraryProperties.getZimDirectory());
        try {
            Files.createDirectories(zimDir);
            Path targetPath = zimDir.resolve(originalFilename);
            Files.copy(file.getInputStream(), targetPath, StandardCopyOption.REPLACE_EXISTING);

            ZimFile zimFile = new ZimFile();
            zimFile.setFilename(originalFilename);
            zimFile.setDisplayName(displayName);
            zimFile.setCategory(category);
            zimFile.setFileSizeBytes(file.getSize());
            zimFile.setFilePath(targetPath.toString());
            zimFile.setUploadedBy(uploadedBy);

            ZimFile saved = zimFileRepository.save(zimFile);
            log.info("ZIM file uploaded: {} ({} bytes) by user {}", originalFilename, file.getSize(), uploadedBy);

            try {
                kiwixProcessService.restart();
            } catch (Exception e) {
                log.warn("Failed to restart kiwix-serve after ZIM upload: {}", e.getMessage());
            }

            return ZimFileDto.from(saved);
        } catch (IOException e) {
            log.error("Failed to save ZIM file to disk: {}", originalFilename, e);
            throw new RuntimeException("Failed to save ZIM file: " + e.getMessage(), e);
        }
    }

    /**
     * Lists all ZIM files ordered by display name.
     *
     * @return list of all ZIM file DTOs
     */
    @Transactional(readOnly = true)
    public List<ZimFileDto> listAll() {
        return zimFileRepository.findAllByOrderByDisplayNameAsc()
                .stream()
                .map(ZimFileDto::from)
                .toList();
    }

    /**
     * Deletes a ZIM file from both disk and database.
     *
     * @param id the ZIM file ID
     * @throws EntityNotFoundException if the ZIM file does not exist
     */
    @Transactional
    public void delete(UUID id) {
        ZimFile zimFile = zimFileRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("ZIM file not found: " + id));

        try {
            Path filePath = Paths.get(zimFile.getFilePath());
            Files.deleteIfExists(filePath);
            log.info("Deleted ZIM file from disk: {}", zimFile.getFilePath());
        } catch (IOException e) {
            log.warn("Failed to delete ZIM file from disk: {}", zimFile.getFilePath(), e);
        }

        zimFileRepository.delete(zimFile);
        log.info("Deleted ZIM file record: {} ({})", zimFile.getFilename(), id);

        try {
            kiwixProcessService.restart();
        } catch (Exception e) {
            log.warn("Failed to restart kiwix-serve after ZIM deletion: {}", e.getMessage());
        }
    }

    /**
     * Returns the base URL for the Kiwix content server.
     *
     * @return the Kiwix server URL
     */
    public String getKiwixServeUrl() {
        return libraryProperties.getKiwixUrl();
    }

    /**
     * Checks the Kiwix server availability and returns its status.
     *
     * @return the Kiwix status DTO
     */
    public KiwixStatusDto getKiwixStatus() {
        int bookCount = (int) zimFileRepository.count();
        boolean processManaged = kiwixProperties.isManageProcess();
        try {
            webClient.get()
                    .uri(libraryProperties.getKiwixUrl())
                    .retrieve()
                    .toBodilessEntity()
                    .block(Duration.ofSeconds(5));
            return new KiwixStatusDto(true, libraryProperties.getKiwixUrl(), bookCount, processManaged);
        } catch (Exception e) {
            log.warn("Kiwix server unreachable at {}: {}", libraryProperties.getKiwixUrl(), e.getMessage());
            return new KiwixStatusDto(false, libraryProperties.getKiwixUrl(), bookCount, processManaged);
        }
    }
}
