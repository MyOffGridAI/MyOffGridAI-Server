package com.myoffgridai.models.service;

import com.myoffgridai.models.dto.HfModelFileDto;
import com.myoffgridai.models.dto.QuantizationType;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for {@link QuantizationRecommendationService}.
 *
 * <p>Tests filename parsing, RAM estimation, recommendation logic,
 * non-model file filtering, and edge cases. Uses the package-private
 * constructor to inject controlled RAM values.</p>
 */
class QuantizationRecommendationServiceTest {

    private static final long GB = 1024L * 1024 * 1024;

    /** 32 GB simulated system RAM */
    private final QuantizationRecommendationService service = new QuantizationRecommendationService(32 * GB);

    // ── Filename Parsing ────────────────────────────────────────────────────

    @ParameterizedTest
    @CsvSource({
            "model-Q4_K_M.gguf, Q4_K_M",
            "model.Q4_K_M.gguf, Q4_K_M",
            "Qwen3.5-27B.Q4_K_S.gguf, Q4_K_S",
            "model-Q2_K.gguf, Q2_K",
            "model-Q3_K_XS.gguf, Q3_K_XS",
            "model-Q3_K_S.gguf, Q3_K_S",
            "model-Q3_K_M.gguf, Q3_K_M",
            "model-Q5_K_S.gguf, Q5_K_S",
            "model-Q5_K_M.gguf, Q5_K_M",
            "model-Q6_K.gguf, Q6_K",
            "model-Q8_0.gguf, Q8_0",
            "model-IQ1_S.gguf, IQ1_S",
            "model-IQ2_XS.gguf, IQ2_XS",
            "model.f16.gguf, F16",
            "model.F16.gguf, F16",
            "model.bf16.gguf, BF16",
            "model.BF16.gguf, BF16",
            "model.f32.gguf, F32",
            "model-F32.gguf, F32"
    })
    void parseQuantType_recognizesKnownFormats(String filename, String expectedName) {
        QuantizationType result = service.parseQuantType(filename);
        assertNotNull(result, "Should parse " + filename);
        assertEquals(QuantizationType.valueOf(expectedName), result);
    }

    @ParameterizedTest
    @ValueSource(strings = {
            "README.md",
            "config.json",
            "tokenizer.json",
            "model.safetensors",
            "model.bin",
            "model.gguf"
    })
    void parseQuantType_returnsNullForUnrecognizedFiles(String filename) {
        assertNull(service.parseQuantType(filename));
    }

    @Test
    void parseQuantType_nullFilename_returnsNull() {
        assertNull(service.parseQuantType(null));
    }

    @Test
    void parseQuantType_caseInsensitive() {
        assertEquals(QuantizationType.Q4_K_M, service.parseQuantType("model-q4_k_m.gguf"));
        assertEquals(QuantizationType.BF16, service.parseQuantType("model-Bf16.gguf"));
    }

    // ── Non-Model File Detection ────────────────────────────────────────────

    @ParameterizedTest
    @ValueSource(strings = {
            "mmproj-BF16.gguf",
            "mmproj-model-f16.gguf",
            "tokenizer.json",
            "tokenizer.model",
            "tokenizer-config.json",
            "config.json"
    })
    void isNonModelFile_detectsNonModelFiles(String filename) {
        assertTrue(service.isNonModelFile(filename),
                filename + " should be detected as non-model file");
    }

    @ParameterizedTest
    @ValueSource(strings = {
            "model-Q4_K_M.gguf",
            "Qwen3.5-27B.Q5_K_M.gguf",
            "model.bf16.gguf",
            "README.md"
    })
    void isNonModelFile_allowsModelFiles(String filename) {
        assertFalse(service.isNonModelFile(filename),
                filename + " should NOT be detected as non-model file");
    }

    @Test
    void isNonModelFile_nullFilename_returnsFalse() {
        assertFalse(service.isNonModelFile(null));
    }

    // ── RAM Estimation ──────────────────────────────────────────────────────

    @Test
    void estimateRam_appliesOverheadFactor() {
        long fileSize = 10 * GB;
        long estimated = service.estimateRam(fileSize);
        assertEquals(12 * GB, estimated);
    }

    @Test
    void estimateRam_zeroFileSize() {
        assertEquals(0L, service.estimateRam(0));
    }

    @Test
    void getAvailableRam_subtractsOsReserve() {
        // 32GB - 4GB = 28GB
        assertEquals(28 * GB, service.getAvailableRam());
    }

    @Test
    void getAvailableRam_neverNegative() {
        QuantizationRecommendationService tinyRam = new QuantizationRecommendationService(2 * GB);
        assertEquals(0L, tinyRam.getAvailableRam());
    }

    // ── enrichFiles ─────────────────────────────────────────────────────────

    @Test
    void enrichFiles_nullList_returnsNull() {
        assertNull(service.enrichFiles(null));
    }

    @Test
    void enrichFiles_emptyList_returnsEmpty() {
        List<HfModelFileDto> result = service.enrichFiles(Collections.emptyList());
        assertTrue(result.isEmpty());
    }

    @Test
    void enrichFiles_populatesQuantizationMetadata() {
        List<HfModelFileDto> files = new ArrayList<>();
        files.add(new HfModelFileDto("model-Q4_K_M.gguf", 4_000_000_000L, "abc123"));

        List<HfModelFileDto> result = service.enrichFiles(files);

        assertEquals(1, result.size());
        HfModelFileDto enriched = result.get(0);
        assertEquals(QuantizationType.Q4_K_M, enriched.quantizationType());
        assertEquals("Medium \u2014 balanced (most popular)", enriched.qualityLabel());
        assertEquals(8, enriched.qualityRank());
        assertEquals(4_800_000_000L, enriched.estimatedRamBytes());
    }

    @Test
    void enrichFiles_setsExactlyOneRecommended() {
        List<HfModelFileDto> files = new ArrayList<>();
        files.add(new HfModelFileDto("model-Q2_K.gguf", 2_000_000_000L, null));
        files.add(new HfModelFileDto("model-Q4_K_M.gguf", 4_000_000_000L, null));
        files.add(new HfModelFileDto("model-Q5_K_M.gguf", 5_000_000_000L, null));
        files.add(new HfModelFileDto("model-Q8_0.gguf", 10_000_000_000L, null));

        List<HfModelFileDto> result = service.enrichFiles(files);

        long recommendedCount = result.stream()
                .filter(f -> Boolean.TRUE.equals(f.recommended()))
                .count();
        assertEquals(1, recommendedCount);
    }

    @Test
    void enrichFiles_recommendsHighestQualityThatFitsInRam() {
        // 32GB RAM - 4GB reserve = 28GB available
        // Q8_0 at 20GB -> estimated 24GB -> fits
        // F16 at 30GB -> estimated 36GB -> does not fit
        List<HfModelFileDto> files = new ArrayList<>();
        files.add(new HfModelFileDto("model-Q4_K_M.gguf", 4_000_000_000L, null));
        files.add(new HfModelFileDto("model-Q8_0.gguf", 20_000_000_000L, null));
        files.add(new HfModelFileDto("model.f16.gguf", 30_000_000_000L, null));

        List<HfModelFileDto> result = service.enrichFiles(files);

        HfModelFileDto recommended = result.stream()
                .filter(f -> Boolean.TRUE.equals(f.recommended()))
                .findFirst()
                .orElseThrow();
        assertEquals(QuantizationType.Q8_0, recommended.quantizationType());
    }

    @Test
    void enrichFiles_fallsBackToSmallestWhenNoneFit() {
        // 8GB RAM - 4GB reserve = 4GB available
        // Q4_K_M at 10GB -> estimated 12GB -> does not fit
        // Q8_0 at 20GB -> estimated 24GB -> does not fit
        QuantizationRecommendationService lowRam = new QuantizationRecommendationService(8 * GB);

        List<HfModelFileDto> files = new ArrayList<>();
        files.add(new HfModelFileDto("model-Q8_0.gguf", 20_000_000_000L, null));
        files.add(new HfModelFileDto("model-Q4_K_M.gguf", 10_000_000_000L, null));

        List<HfModelFileDto> result = lowRam.enrichFiles(files);

        HfModelFileDto recommended = result.stream()
                .filter(f -> Boolean.TRUE.equals(f.recommended()))
                .findFirst()
                .orElseThrow();
        // Falls back to smallest by estimated RAM
        assertEquals(QuantizationType.Q4_K_M, recommended.quantizationType());
    }

    @Test
    void enrichFiles_nonModelFilesPassedThroughUnchanged() {
        List<HfModelFileDto> files = new ArrayList<>();
        files.add(new HfModelFileDto("mmproj-BF16.gguf", 1_000_000_000L, null));
        files.add(new HfModelFileDto("model-Q4_K_M.gguf", 4_000_000_000L, null));

        List<HfModelFileDto> result = service.enrichFiles(files);

        assertEquals(2, result.size());
        // mmproj file should have no quantization metadata
        HfModelFileDto mmproj = result.get(0);
        assertNull(mmproj.quantizationType());
        assertNull(mmproj.qualityLabel());
        assertNull(mmproj.qualityRank());
        assertNull(mmproj.estimatedRamBytes());
        assertNull(mmproj.recommended());
    }

    @Test
    void enrichFiles_unrecognizedGgufPassedThroughUnchanged() {
        List<HfModelFileDto> files = new ArrayList<>();
        files.add(new HfModelFileDto("model.gguf", 4_000_000_000L, null));

        List<HfModelFileDto> result = service.enrichFiles(files);

        HfModelFileDto file = result.get(0);
        assertNull(file.quantizationType());
        assertNull(file.recommended());
    }

    @Test
    void enrichFiles_noSizeField_omitsRamEstimate() {
        List<HfModelFileDto> files = new ArrayList<>();
        files.add(new HfModelFileDto("model-Q4_K_M.gguf", null, null));

        List<HfModelFileDto> result = service.enrichFiles(files);

        HfModelFileDto enriched = result.get(0);
        assertEquals(QuantizationType.Q4_K_M, enriched.quantizationType());
        assertNull(enriched.estimatedRamBytes());
    }

    @Test
    void enrichFiles_preservesBlobId() {
        List<HfModelFileDto> files = new ArrayList<>();
        files.add(new HfModelFileDto("model-Q4_K_M.gguf", 4_000_000_000L, "blob123"));

        List<HfModelFileDto> result = service.enrichFiles(files);

        assertEquals("blob123", result.get(0).blobId());
    }

    @Test
    void enrichFiles_mixedGgufAndNonGguf() {
        List<HfModelFileDto> files = new ArrayList<>();
        files.add(new HfModelFileDto("README.md", 1000L, null));
        files.add(new HfModelFileDto("model-Q4_K_M.gguf", 4_000_000_000L, null));
        files.add(new HfModelFileDto("config.json", 500L, null));

        List<HfModelFileDto> result = service.enrichFiles(files);

        assertEquals(3, result.size());
        // Only the GGUF file should have quant metadata
        assertNull(result.get(0).quantizationType());
        assertEquals(QuantizationType.Q4_K_M, result.get(1).quantizationType());
        assertNull(result.get(2).quantizationType());
    }

    @Test
    void enrichFiles_singleFile_isRecommended() {
        List<HfModelFileDto> files = new ArrayList<>();
        files.add(new HfModelFileDto("model-Q4_K_M.gguf", 4_000_000_000L, null));

        List<HfModelFileDto> result = service.enrichFiles(files);

        assertTrue(Boolean.TRUE.equals(result.get(0).recommended()));
    }

    @Test
    void enrichFiles_allFilesWithoutSize_noRecommendation() {
        List<HfModelFileDto> files = new ArrayList<>();
        files.add(new HfModelFileDto("model-Q4_K_M.gguf", null, null));
        files.add(new HfModelFileDto("model-Q5_K_M.gguf", null, null));

        List<HfModelFileDto> result = service.enrichFiles(files);

        long recommendedCount = result.stream()
                .filter(f -> Boolean.TRUE.equals(f.recommended()))
                .count();
        assertEquals(0, recommendedCount);
    }
}
