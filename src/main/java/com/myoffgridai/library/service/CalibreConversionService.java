package com.myoffgridai.library.service;

import com.myoffgridai.config.AppConstants;
import com.myoffgridai.library.model.EbookFormat;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.nio.file.Path;
import java.util.concurrent.TimeUnit;

/**
 * Service for converting eBook formats using Calibre's ebook-convert tool
 * running inside the {@code myoffgridai-calibre} Docker container.
 *
 * <p>Translates host file paths to container paths (host {@code ./library/ebooks/}
 * maps to container {@code /books/}) before executing conversions.</p>
 */
@Service
public class CalibreConversionService {

    private static final Logger log = LoggerFactory.getLogger(CalibreConversionService.class);

    /**
     * Converts an eBook file to EPUB format using Calibre.
     *
     * <p>The source file must exist in the ebooks directory. The converted
     * EPUB file is written alongside the original with a {@code .epub} extension.</p>
     *
     * @param sourcePath   the host-side path to the source file
     * @param sourceFormat the source file format
     * @return the path to the converted EPUB file, or null if conversion failed
     */
    public Path convertToEpub(Path sourcePath, EbookFormat sourceFormat) {
        if (sourceFormat == EbookFormat.EPUB) {
            log.debug("File is already EPUB, skipping conversion: {}", sourcePath);
            return sourcePath;
        }

        String filename = sourcePath.getFileName().toString();
        String containerSourcePath = "/books/" + filename;
        String epubFilename = filename.replaceFirst("\\.[^.]+$", ".epub");
        String containerTargetPath = "/books/" + epubFilename;

        try {
            ProcessBuilder pb = new ProcessBuilder(
                    "docker", "exec", "myoffgridai-calibre",
                    "ebook-convert", containerSourcePath, containerTargetPath
            );
            pb.redirectErrorStream(true);

            log.info("Starting Calibre conversion: {} -> {}", containerSourcePath, containerTargetPath);
            Process process = pb.start();

            StringBuilder output = new StringBuilder();
            try (BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()))) {
                String line;
                while ((line = reader.readLine()) != null) {
                    output.append(line).append("\n");
                }
            }

            boolean completed = process.waitFor(AppConstants.CALIBRE_CONVERSION_TIMEOUT_SECONDS, TimeUnit.SECONDS);
            if (!completed) {
                process.destroyForcibly();
                log.error("Calibre conversion timed out after {}s for: {}",
                        AppConstants.CALIBRE_CONVERSION_TIMEOUT_SECONDS, filename);
                return null;
            }

            if (process.exitValue() != 0) {
                log.error("Calibre conversion failed (exit code {}): {}\nOutput: {}",
                        process.exitValue(), filename, output);
                return null;
            }

            Path convertedPath = sourcePath.getParent().resolve(epubFilename);
            log.info("Calibre conversion completed: {} -> {}", filename, epubFilename);
            return convertedPath;

        } catch (Exception e) {
            log.error("Calibre conversion error for {}: {}", filename, e.getMessage(), e);
            return null;
        }
    }

    /**
     * Checks whether the Calibre Docker container is available.
     *
     * @return true if the container is running and ebook-convert is accessible
     */
    public boolean isAvailable() {
        try {
            ProcessBuilder pb = new ProcessBuilder(
                    "docker", "exec", "myoffgridai-calibre",
                    "ebook-convert", "--version"
            );
            pb.redirectErrorStream(true);

            Process process = pb.start();
            boolean completed = process.waitFor(10, TimeUnit.SECONDS);
            return completed && process.exitValue() == 0;
        } catch (Exception e) {
            log.debug("Calibre container not available: {}", e.getMessage());
            return false;
        }
    }
}
