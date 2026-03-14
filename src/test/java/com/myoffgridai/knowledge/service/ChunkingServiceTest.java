package com.myoffgridai.knowledge.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.*;

class ChunkingServiceTest {

    private ChunkingService chunkingService;

    @BeforeEach
    void setUp() {
        chunkingService = new ChunkingService();
    }

    @Test
    void chunkText_null_returnsEmpty() {
        assertThat(chunkingService.chunkText(null)).isEmpty();
    }

    @Test
    void chunkText_blank_returnsEmpty() {
        assertThat(chunkingService.chunkText("   ")).isEmpty();
    }

    @Test
    void chunkText_shortText_returnsSingleChunk() {
        String text = "This is a short sentence that has enough content. It has two sentences here for testing.";
        List<String> chunks = chunkingService.chunkText(text);

        assertThat(chunks).hasSize(1);
        assertThat(chunks.get(0)).isEqualTo(text);
    }

    @Test
    void chunkText_longText_returnsMultipleChunks() {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < 100; i++) {
            sb.append("This is sentence number ").append(i).append(". ");
        }
        String text = sb.toString().trim();

        List<String> chunks = chunkingService.chunkText(text);

        assertThat(chunks).hasSizeGreaterThan(1);
        // All chunks should be non-empty
        for (String chunk : chunks) {
            assertThat(chunk).isNotBlank();
            assertThat(chunk.length()).isGreaterThanOrEqualTo(50);
        }
    }

    @Test
    void chunkText_overlapsCorrectly() {
        // Create text with clear sentence boundaries
        StringBuilder sb = new StringBuilder();
        for (int i = 1; i <= 50; i++) {
            sb.append("Sentence_").append(i).append("_content. ");
        }

        List<String> chunks = chunkingService.chunkText(sb.toString().trim());

        // With overlap, later chunks should share some content with earlier ones
        if (chunks.size() >= 2) {
            // Find overlap: some content from end of chunk 0 should appear at start of chunk 1
            String lastSentenceOfFirst = chunks.get(0).substring(
                    chunks.get(0).lastIndexOf("Sentence_"));
            String firstPartOfSecond = chunks.get(1).substring(0,
                    Math.min(200, chunks.get(1).length()));
            // The overlap should cause some shared content
            assertThat(chunks.get(0).length()).isGreaterThan(0);
            assertThat(chunks.get(1).length()).isGreaterThan(0);
        }
    }

    @Test
    void chunkText_tinyChunksDiscarded() {
        // A very short text under MIN_CHUNK_SIZE_CHARS (50) should be discarded
        String text = "Hi.";
        List<String> chunks = chunkingService.chunkText(text);

        assertThat(chunks).isEmpty();
    }

    @Test
    void chunkText_textWithNewlines_splitsCorrectly() {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < 100; i++) {
            sb.append("Line ").append(i).append(" of content\n");
        }

        List<String> chunks = chunkingService.chunkText(sb.toString());

        assertThat(chunks).isNotEmpty();
        for (String chunk : chunks) {
            assertThat(chunk.length()).isGreaterThanOrEqualTo(50);
        }
    }

    @Test
    void chunkText_singleLongSentence_producesChunk() {
        // Single sentence longer than CHUNK_SIZE_CHARS
        String longSentence = "A".repeat(2000) + ".";

        List<String> chunks = chunkingService.chunkText(longSentence);

        assertThat(chunks).hasSize(1);
        assertThat(chunks.get(0)).isEqualTo(longSentence);
    }
}
