package com.myoffgridai.knowledge.dto;

import java.util.UUID;

/**
 * Response DTO containing a document's editable content.
 *
 * @param documentId the document ID
 * @param title      the display name or filename
 * @param content    the Quill Delta JSON content
 * @param mimeType   the document's MIME type
 * @param editable   whether the document supports editing
 */
public record DocumentContentDto(
        UUID documentId,
        String title,
        String content,
        String mimeType,
        boolean editable
) {
}
