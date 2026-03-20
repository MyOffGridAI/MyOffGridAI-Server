package com.myoffgridai.library.dto;

import jakarta.validation.constraints.NotBlank;

/**
 * Request body for initiating a ZIM file download from the Kiwix catalog.
 *
 * @param downloadUrl the direct download URL for the ZIM file
 * @param filename    the target filename for the downloaded ZIM
 * @param displayName the human-readable display name
 * @param category    the content category
 * @param language    the content language
 * @param sizeBytes   the expected file size in bytes
 */
public record KiwixCatalogDownloadRequest(
        @NotBlank String downloadUrl,
        @NotBlank String filename,
        @NotBlank String displayName,
        String category,
        String language,
        long sizeBytes
) {
}
