package com.myoffgridai.knowledge.dto;

import com.myoffgridai.knowledge.model.DocumentStatus;

import java.time.Instant;
import java.util.UUID;

/**
 * Response DTO for a knowledge document.
 *
 * @param id            the document ID
 * @param filename      the original filename
 * @param displayName   the user-defined display name (may be null)
 * @param mimeType      the MIME type of the uploaded file
 * @param fileSizeBytes the file size in bytes
 * @param status        the processing status
 * @param errorMessage  error message if processing failed (may be null)
 * @param chunkCount    the number of chunks produced
 * @param uploadedAt    the upload timestamp
 * @param processedAt   the processing completion timestamp (may be null)
 * @param hasContent    whether extracted content is available for viewing
 * @param editable      whether the document can be edited in the rich text editor
 */
public record KnowledgeDocumentDto(
        UUID id,
        String filename,
        String displayName,
        String mimeType,
        long fileSizeBytes,
        DocumentStatus status,
        String errorMessage,
        int chunkCount,
        Instant uploadedAt,
        Instant processedAt,
        boolean hasContent,
        boolean editable
) {
}
