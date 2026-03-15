package com.myoffgridai.knowledge.dto;

import jakarta.validation.constraints.NotBlank;

/**
 * Request DTO for creating a new document from the rich text editor.
 *
 * @param title   the document title
 * @param content the Quill Delta JSON content
 */
public record CreateDocumentRequest(
        @NotBlank(message = "Title is required") String title,
        @NotBlank(message = "Content is required") String content
) {
}
