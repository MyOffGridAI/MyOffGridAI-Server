package com.myoffgridai.models.service;

import com.myoffgridai.config.AppConstants;
import com.myoffgridai.models.dto.HfModelFileDto;
import com.myoffgridai.models.dto.QuantizationType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.lang.management.ManagementFactory;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Enriches GGUF model file listings with parsed quantization metadata,
 * human-readable labels, RAM estimates, and a recommended variant flag.
 *
 * <p>For each file in a model repository, this service:
 * <ul>
 *   <li>Parses the quantization type from the GGUF filename</li>
 *   <li>Estimates RAM required to load the model (file size x overhead factor)</li>
 *   <li>Selects the highest-quality variant that fits in available system RAM</li>
 *   <li>Filters out non-model files (mmproj, tokenizer, config)</li>
 * </ul>
 *
 * <p>The recommended variant is the highest-quality quantization whose estimated
 * RAM fits within {@code (totalSystemRAM - 4GB OS reserve)}.</p>
 */
@Service
public class QuantizationRecommendationService {

    private static final Logger log = LoggerFactory.getLogger(QuantizationRecommendationService.class);

    /**
     * Regex to extract quantization type from GGUF filenames.
     *
     * <p>Matches patterns like {@code model-Q4_K_M.gguf}, {@code model.bf16.gguf},
     * {@code model.Q8_0.gguf} with case-insensitive matching.</p>
     */
    static final Pattern QUANT_PATTERN = Pattern.compile(
            "(?i)[-.](" +
                    "IQ1_S|IQ2_XS|" +
                    "Q2_K|Q3_K_XS|Q3_K_S|Q3_K_M|" +
                    "Q4_K_S|Q4_K_M|Q5_K_S|Q5_K_M|" +
                    "Q6_K|Q8_0|" +
                    "BF16|F16|F32" +
                    ")\\.gguf$"
    );

    /**
     * Pattern to identify non-model GGUF files that should not receive quantization metadata.
     */
    static final Pattern NON_MODEL_PATTERN = Pattern.compile(
            "(?i)^mmproj-.*\\.gguf$|^tokenizer[._-]|^config\\."
    );

    private final long totalSystemRamBytes;

    /**
     * Constructs the service, detecting system RAM automatically.
     */
    public QuantizationRecommendationService() {
        this.totalSystemRamBytes = detectSystemRam();
        log.info("System RAM detected: {} GB", totalSystemRamBytes / (1024L * 1024 * 1024));
    }

    /**
     * Package-private constructor for testing with a controlled RAM value.
     *
     * @param totalSystemRamBytes the simulated total system RAM in bytes
     */
    QuantizationRecommendationService(long totalSystemRamBytes) {
        this.totalSystemRamBytes = totalSystemRamBytes;
    }

    /**
     * Enriches a list of model files with quantization metadata and recommends
     * the best variant for the current system.
     *
     * <p>Non-model files (mmproj, tokenizer, config) are passed through unchanged.
     * Model files without a recognized quantization type are also passed through.
     * Exactly one file is marked as {@code recommended=true} — the highest-quality
     * variant that fits in available RAM.</p>
     *
     * @param files the raw file list from a HuggingFace model repository
     * @return the enriched file list with quantization metadata and recommendation
     */
    public List<HfModelFileDto> enrichFiles(List<HfModelFileDto> files) {
        if (files == null || files.isEmpty()) {
            return files;
        }

        List<HfModelFileDto> enriched = new ArrayList<>(files.size());
        for (HfModelFileDto file : files) {
            enriched.add(enrichSingleFile(file));
        }

        markRecommended(enriched);
        return enriched;
    }

    /**
     * Parses the quantization type from a GGUF filename.
     *
     * @param filename the filename to parse (e.g. "model-Q4_K_M.gguf")
     * @return the parsed quantization type, or null if not recognized
     */
    QuantizationType parseQuantType(String filename) {
        if (filename == null) {
            return null;
        }
        Matcher matcher = QUANT_PATTERN.matcher(filename);
        if (matcher.find()) {
            String quantStr = matcher.group(1).toUpperCase();
            try {
                return QuantizationType.valueOf(quantStr);
            } catch (IllegalArgumentException e) {
                log.debug("Unrecognized quantization type in filename: {}", filename);
                return null;
            }
        }
        return null;
    }

    /**
     * Estimates the RAM required to load a model file.
     *
     * @param fileSize the file size in bytes
     * @return the estimated RAM in bytes, applying the overhead factor
     */
    long estimateRam(long fileSize) {
        return (long) (fileSize * AppConstants.MODEL_RAM_OVERHEAD_FACTOR);
    }

    /**
     * Checks whether a filename represents a non-model file that should be skipped.
     *
     * @param filename the filename to check
     * @return true if the file is a non-model file (mmproj, tokenizer, config)
     */
    boolean isNonModelFile(String filename) {
        if (filename == null) {
            return false;
        }
        return NON_MODEL_PATTERN.matcher(filename).find();
    }

    /**
     * Returns the available RAM for model loading (total minus OS reserve).
     *
     * @return available RAM in bytes
     */
    long getAvailableRam() {
        return Math.max(0, totalSystemRamBytes - AppConstants.MODEL_RAM_OS_RESERVE_BYTES);
    }

    private HfModelFileDto enrichSingleFile(HfModelFileDto file) {
        if (isNonModelFile(file.rfilename())) {
            return file;
        }

        QuantizationType quantType = parseQuantType(file.rfilename());
        if (quantType == null) {
            return file;
        }

        Long estimatedRam = null;
        if (file.size() != null) {
            estimatedRam = estimateRam(file.size());
        }

        return new HfModelFileDto(
                file.rfilename(),
                file.size(),
                file.blobId(),
                quantType,
                quantType.getLabel(),
                quantType.getRank(),
                estimatedRam,
                null
        );
    }

    private void markRecommended(List<HfModelFileDto> files) {
        long availableRam = getAvailableRam();

        // Find enriched files with RAM estimates, sorted by rank descending
        HfModelFileDto bestFit = files.stream()
                .filter(f -> f.quantizationType() != null)
                .filter(f -> f.estimatedRamBytes() != null)
                .filter(f -> f.estimatedRamBytes() <= availableRam)
                .max(Comparator.comparingInt(HfModelFileDto::qualityRank))
                .orElse(null);

        if (bestFit == null) {
            // No file fits — recommend the smallest enriched file as a fallback
            bestFit = files.stream()
                    .filter(f -> f.quantizationType() != null)
                    .filter(f -> f.estimatedRamBytes() != null)
                    .min(Comparator.comparingLong(HfModelFileDto::estimatedRamBytes))
                    .orElse(null);
        }

        if (bestFit == null) {
            return;
        }

        // Replace the best-fit file with a copy that has recommended=true
        String recommendedFilename = bestFit.rfilename();
        for (int i = 0; i < files.size(); i++) {
            HfModelFileDto f = files.get(i);
            if (recommendedFilename.equals(f.rfilename())) {
                files.set(i, f.withRecommended(true));
                log.debug("Recommended variant: {} (rank={}, RAM={}GB)",
                        f.rfilename(), f.qualityRank(),
                        f.estimatedRamBytes() != null ? f.estimatedRamBytes() / (1024L * 1024 * 1024) : "?");
                break;
            }
        }
    }

    private long detectSystemRam() {
        try {
            com.sun.management.OperatingSystemMXBean osBean =
                    (com.sun.management.OperatingSystemMXBean) ManagementFactory.getOperatingSystemMXBean();
            return osBean.getTotalMemorySize();
        } catch (Exception e) {
            log.warn("Could not detect system RAM, defaulting to 16GB: {}", e.getMessage());
            return 16L * 1024 * 1024 * 1024;
        }
    }
}
