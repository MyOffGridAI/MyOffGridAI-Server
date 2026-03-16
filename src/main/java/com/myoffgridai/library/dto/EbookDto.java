package com.myoffgridai.library.dto;

import com.myoffgridai.library.model.Ebook;
import com.myoffgridai.library.model.EbookFormat;

import java.time.Instant;
import java.util.UUID;

/**
 * Data transfer object for eBook metadata.
 *
 * <p>Excludes internal file paths (filePath, coverImagePath) and
 * exposes a {@code hasCoverImage} boolean instead.</p>
 *
 * @param id             the unique identifier
 * @param title          the book title
 * @param author         the author name
 * @param description    the book description
 * @param isbn           the ISBN (if available)
 * @param publisher      the publisher name
 * @param publishedYear  the publication year
 * @param language       the content language
 * @param format         the file format
 * @param fileSizeBytes  the file size in bytes
 * @param gutenbergId    the Project Gutenberg ID (if imported)
 * @param downloadCount  the number of times this book has been downloaded
 * @param hasCoverImage  whether a cover image is available
 * @param uploadedAt     the upload timestamp
 * @param uploadedBy     the ID of the user who uploaded/imported the book
 */
public record EbookDto(
        UUID id,
        String title,
        String author,
        String description,
        String isbn,
        String publisher,
        Integer publishedYear,
        String language,
        EbookFormat format,
        long fileSizeBytes,
        String gutenbergId,
        int downloadCount,
        boolean hasCoverImage,
        Instant uploadedAt,
        UUID uploadedBy
) {

    /**
     * Creates a DTO from an Ebook entity.
     *
     * @param entity the ebook entity
     * @return the corresponding DTO
     */
    public static EbookDto from(Ebook entity) {
        return new EbookDto(
                entity.getId(),
                entity.getTitle(),
                entity.getAuthor(),
                entity.getDescription(),
                entity.getIsbn(),
                entity.getPublisher(),
                entity.getPublishedYear(),
                entity.getLanguage(),
                entity.getFormat(),
                entity.getFileSizeBytes(),
                entity.getGutenbergId(),
                entity.getDownloadCount(),
                entity.getCoverImagePath() != null && !entity.getCoverImagePath().isBlank(),
                entity.getUploadedAt(),
                entity.getUploadedBy()
        );
    }
}
