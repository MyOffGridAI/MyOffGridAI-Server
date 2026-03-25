package com.myoffgridai.library.service;

import com.myoffgridai.config.AppConstants;
import com.myoffgridai.library.config.LibraryProperties;
import com.myoffgridai.library.dto.EbookDto;
import com.myoffgridai.library.model.Ebook;
import com.myoffgridai.library.model.EbookFormat;
import com.myoffgridai.library.repository.EbookRepository;
import jakarta.persistence.EntityNotFoundException;
import org.apache.commons.io.FilenameUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import org.springframework.core.io.InputStreamResource;
import org.springframework.core.io.Resource;

import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.UUID;

/**
 * Service for managing eBooks in the offline library.
 *
 * <p>Handles upload with format detection, optional Calibre conversion
 * for MOBI/AZW files, listing with search and format filtering,
 * deletion, and download count tracking.</p>
 */
@Service
public class EbookService {

    private static final Logger log = LoggerFactory.getLogger(EbookService.class);

    private final EbookRepository ebookRepository;
    private final LibraryProperties libraryProperties;
    private final CalibreConversionService calibreConversionService;

    /**
     * Constructs the eBook service.
     *
     * @param ebookRepository          the eBook repository
     * @param libraryProperties        the library configuration properties
     * @param calibreConversionService the Calibre conversion service
     */
    public EbookService(EbookRepository ebookRepository,
                        LibraryProperties libraryProperties,
                        CalibreConversionService calibreConversionService) {
        this.ebookRepository = ebookRepository;
        this.libraryProperties = libraryProperties;
        this.calibreConversionService = calibreConversionService;
    }

    /**
     * Uploads an eBook file to the library.
     *
     * <p>Detects the format from the file extension, validates against
     * supported formats and size limits, saves to disk, and optionally
     * triggers Calibre conversion for MOBI/AZW files.</p>
     *
     * @param file       the uploaded file
     * @param title      the book title
     * @param author     the author name (nullable)
     * @param uploadedBy the ID of the uploading user
     * @return the created eBook DTO
     * @throws IllegalArgumentException if validation fails
     * @throws RuntimeException         if file I/O fails
     */
    @Transactional
    public EbookDto upload(MultipartFile file, String title, String author, UUID uploadedBy) {
        String originalFilename = file.getOriginalFilename();
        String extension = FilenameUtils.getExtension(originalFilename);

        EbookFormat format = detectFormat(extension);
        if (format == null) {
            throw new IllegalArgumentException(
                    "Unsupported eBook format: ." + extension + ". Supported: " + AppConstants.SUPPORTED_EBOOK_EXTENSIONS);
        }

        if (file.getSize() > AppConstants.MAX_EBOOK_UPLOAD_BYTES) {
            throw new IllegalArgumentException(
                    String.format("File size (%d MB) exceeds the maximum allowed (%d MB)",
                            file.getSize() / (1024 * 1024), AppConstants.MAX_EBOOK_UPLOAD_BYTES / (1024 * 1024)));
        }

        Path ebookDir = Paths.get(libraryProperties.getEbookDirectory());
        try {
            Files.createDirectories(ebookDir);
            String safeFilename = UUID.randomUUID() + "-" + originalFilename;
            Path targetPath = ebookDir.resolve(safeFilename);
            Files.copy(file.getInputStream(), targetPath, StandardCopyOption.REPLACE_EXISTING);

            // Attempt Calibre conversion for MOBI/AZW to EPUB
            if (format == EbookFormat.MOBI || format == EbookFormat.AZW) {
                if (calibreConversionService.isAvailable()) {
                    Path converted = calibreConversionService.convertToEpub(targetPath, format);
                    if (converted != null && !converted.equals(targetPath)) {
                        targetPath = converted;
                        format = EbookFormat.EPUB;
                        log.info("MOBI/AZW converted to EPUB: {}", converted.getFileName());
                    } else {
                        log.warn("Calibre conversion failed for {}, storing in original format", originalFilename);
                    }
                } else {
                    log.warn("Calibre unavailable, storing {} in original format", originalFilename);
                }
            }

            Ebook ebook = new Ebook();
            ebook.setTitle(title);
            ebook.setAuthor(author);
            ebook.setFormat(format);
            ebook.setFileSizeBytes(Files.size(targetPath));
            ebook.setFilePath(targetPath.toString());
            ebook.setUploadedBy(uploadedBy);

            Ebook saved = ebookRepository.save(ebook);
            log.info("eBook uploaded: '{}' ({}) by user {}", title, format, uploadedBy);
            return EbookDto.from(saved);
        } catch (IOException e) {
            log.error("Failed to save eBook to disk: {}", originalFilename, e);
            throw new RuntimeException("Failed to save eBook: " + e.getMessage(), e);
        }
    }

    /**
     * Lists eBooks with optional search and format filter.
     *
     * @param search   the search term for title/author (nullable)
     * @param format   the format filter (nullable)
     * @param pageable pagination parameters
     * @return a page of eBook DTOs
     */
    @Transactional(readOnly = true)
    public Page<EbookDto> list(String search, EbookFormat format, Pageable pageable) {
        return ebookRepository.searchByTitleOrAuthor(search, format, pageable)
                .map(EbookDto::from);
    }

    /**
     * Gets a single eBook by ID.
     *
     * @param id the eBook ID
     * @return the eBook DTO
     * @throws EntityNotFoundException if not found
     */
    @Transactional(readOnly = true)
    public EbookDto get(UUID id) {
        Ebook ebook = ebookRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("eBook not found: " + id));
        return EbookDto.from(ebook);
    }

    /**
     * Gets the cover image file for an eBook as a {@link Resource}.
     *
     * @param id the eBook ID
     * @return the cover image resource
     * @throws EntityNotFoundException if the eBook is not found
     * @throws FileNotFoundException   if the eBook has no cover image or the file is missing
     */
    @Transactional(readOnly = true)
    public Resource getCoverFile(UUID id) throws FileNotFoundException {
        Ebook ebook = ebookRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("eBook not found: " + id));

        if (ebook.getCoverImagePath() == null || ebook.getCoverImagePath().isBlank()) {
            throw new FileNotFoundException("No cover image for eBook: " + id);
        }

        Path coverPath = Paths.get(libraryProperties.getEbookDirectory()).resolve(ebook.getCoverImagePath());
        if (!Files.exists(coverPath)) {
            throw new FileNotFoundException("Cover image file missing: " + coverPath);
        }

        return new InputStreamResource(new FileInputStream(coverPath.toFile()));
    }

    /**
     * Deletes an eBook from both disk and database.
     *
     * @param id the eBook ID
     * @throws EntityNotFoundException if not found
     */
    @Transactional
    public void delete(UUID id) {
        Ebook ebook = ebookRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("eBook not found: " + id));

        try {
            Path filePath = Paths.get(ebook.getFilePath());
            Files.deleteIfExists(filePath);
            log.info("Deleted eBook file from disk: {}", ebook.getFilePath());
        } catch (IOException e) {
            log.warn("Failed to delete eBook file from disk: {}", ebook.getFilePath(), e);
        }

        // Delete cover image file if present
        if (ebook.getCoverImagePath() != null && !ebook.getCoverImagePath().isBlank()) {
            try {
                Path coverPath = Paths.get(libraryProperties.getEbookDirectory())
                        .resolve(ebook.getCoverImagePath());
                Files.deleteIfExists(coverPath);
                log.info("Deleted cover image from disk: {}", ebook.getCoverImagePath());
            } catch (IOException e) {
                log.warn("Failed to delete cover image from disk: {}", ebook.getCoverImagePath(), e);
            }
        }

        ebookRepository.delete(ebook);
        log.info("Deleted eBook record: '{}' ({})", ebook.getTitle(), id);
    }

    /**
     * Gets an eBook entity for file download, incrementing the download count.
     *
     * @param id the eBook ID
     * @return the eBook entity (with filePath for streaming)
     * @throws EntityNotFoundException if not found
     */
    @Transactional
    public Ebook getForDownload(UUID id) {
        Ebook ebook = ebookRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("eBook not found: " + id));
        ebook.setDownloadCount(ebook.getDownloadCount() + 1);
        ebookRepository.save(ebook);
        return ebook;
    }

    /**
     * Detects the eBook format from a file extension.
     *
     * @param extension the file extension (without dot)
     * @return the detected format, or null if unsupported
     */
    EbookFormat detectFormat(String extension) {
        if (extension == null) {
            return null;
        }
        return switch (extension.toLowerCase()) {
            case "epub" -> EbookFormat.EPUB;
            case "pdf" -> EbookFormat.PDF;
            case "mobi" -> EbookFormat.MOBI;
            case "azw" -> EbookFormat.AZW;
            case "txt" -> EbookFormat.TXT;
            case "html", "htm" -> EbookFormat.HTML;
            default -> null;
        };
    }
}
