package com.myoffgridai.knowledge.dto;

import jakarta.validation.constraints.NotBlank;

/**
 * Request DTO for updating a document's content from the rich text editor.
 *
 * @param content the updated Quill Delta JSON content
 */
public record UpdateContentRequest(
        @NotBlank(message = "Content is required") String content
) {
}
