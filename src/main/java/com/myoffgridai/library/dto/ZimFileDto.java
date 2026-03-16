package com.myoffgridai.library.dto;

import com.myoffgridai.library.model.ZimFile;

import java.time.Instant;
import java.util.UUID;

/**
 * Data transfer object for ZIM file metadata.
 *
 * @param id            the unique identifier
 * @param filename      the original filename on disk
 * @param displayName   the human-readable display name
 * @param description   optional description
 * @param language      the content language code
 * @param category      the content category (e.g., "wikipedia", "medical")
 * @param fileSizeBytes the file size in bytes
 * @param articleCount  the number of articles in the ZIM file
 * @param mediaCount    the number of media items in the ZIM file
 * @param createdDate   the creation date string from ZIM metadata
 * @param kiwixBookId   the Kiwix book identifier
 * @param uploadedAt    the upload timestamp
 * @param uploadedBy    the ID of the user who uploaded the file
 */
public record ZimFileDto(
        UUID id,
        String filename,
        String displayName,
        String description,
        String language,
        String category,
        long fileSizeBytes,
        int articleCount,
        int mediaCount,
        String createdDate,
        String kiwixBookId,
        Instant uploadedAt,
        UUID uploadedBy
) {

    /**
     * Creates a DTO from a ZimFile entity.
     *
     * @param entity the ZIM file entity
     * @return the corresponding DTO
     */
    public static ZimFileDto from(ZimFile entity) {
        return new ZimFileDto(
                entity.getId(),
                entity.getFilename(),
                entity.getDisplayName(),
                entity.getDescription(),
                entity.getLanguage(),
                entity.getCategory(),
                entity.getFileSizeBytes(),
                entity.getArticleCount(),
                entity.getMediaCount(),
                entity.getCreatedDate(),
                entity.getKiwixBookId(),
                entity.getUploadedAt(),
                entity.getUploadedBy()
        );
    }
}
