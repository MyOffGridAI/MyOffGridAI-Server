package com.myoffgridai.knowledge.service;

import com.myoffgridai.config.AppConstants;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

/**
 * Splits extracted text into overlapping chunks for embedding and retrieval.
 *
 * <p>Uses sentence-boundary splitting with configurable target size and overlap.
 * Chunks smaller than {@link AppConstants#MIN_CHUNK_SIZE_CHARS} are discarded.
 * The total number of chunks is capped at {@link AppConstants#MAX_CHUNKS_PER_DOCUMENT}.</p>
 */
@Service
public class ChunkingService {

    private static final Logger log = LoggerFactory.getLogger(ChunkingService.class);

    /**
     * Splits the given text into overlapping chunks.
     *
     * @param text the full text to chunk
     * @return a list of chunk strings
     */
    public List<String> chunkText(String text) {
        if (text == null || text.isBlank()) {
            return List.of();
        }

        List<String> sentences = splitIntoSentences(text);
        List<String> chunks = new ArrayList<>();

        int i = 0;
        while (i < sentences.size() && chunks.size() < AppConstants.MAX_CHUNKS_PER_DOCUMENT) {
            StringBuilder chunk = new StringBuilder();
            int sentenceStart = i;

            // Build chunk up to target size
            while (i < sentences.size()
                    && chunk.length() + sentences.get(i).length() <= AppConstants.CHUNK_SIZE_CHARS) {
                if (chunk.length() > 0) {
                    chunk.append(" ");
                }
                chunk.append(sentences.get(i));
                i++;
            }

            // If we couldn't add any sentence (single sentence exceeds target),
            // hard-split it at word boundaries into CHUNK_SIZE_CHARS pieces
            if (i == sentenceStart && i < sentences.size()) {
                String oversized = sentences.get(i);
                int pos = 0;
                while (pos < oversized.length() && chunks.size() < AppConstants.MAX_CHUNKS_PER_DOCUMENT) {
                    int end = Math.min(pos + AppConstants.CHUNK_SIZE_CHARS, oversized.length());
                    // Try to break at a word boundary
                    if (end < oversized.length()) {
                        int lastSpace = oversized.lastIndexOf(' ', end);
                        if (lastSpace > pos) {
                            end = lastSpace;
                        }
                    }
                    String piece = oversized.substring(pos, end).trim();
                    if (piece.length() >= AppConstants.MIN_CHUNK_SIZE_CHARS) {
                        chunks.add(piece);
                    }
                    pos = end;
                }
                i++;
                continue;
            }

            String chunkText = chunk.toString().trim();
            if (chunkText.length() >= AppConstants.MIN_CHUNK_SIZE_CHARS) {
                chunks.add(chunkText);
            }

            // Back up for overlap
            if (i < sentences.size()) {
                int overlapChars = 0;
                int backtrack = i - 1;
                while (backtrack > sentenceStart && overlapChars < AppConstants.CHUNK_OVERLAP_CHARS) {
                    overlapChars += sentences.get(backtrack).length();
                    backtrack--;
                }
                i = backtrack + 1;
            }
        }

        log.debug("Split text ({} chars) into {} chunks", text.length(), chunks.size());
        return chunks;
    }

    private List<String> splitIntoSentences(String text) {
        List<String> sentences = new ArrayList<>();
        // Split on sentence-ending punctuation followed by whitespace
        String[] parts = text.split("(?<=[.!?])\\s+");
        for (String part : parts) {
            String trimmed = part.trim();
            if (!trimmed.isEmpty()) {
                sentences.add(trimmed);
            }
        }
        // If no sentence boundaries found, split by newlines
        if (sentences.size() <= 1 && text.length() > AppConstants.CHUNK_SIZE_CHARS) {
            sentences.clear();
            String[] lines = text.split("\\n+");
            for (String line : lines) {
                String trimmed = line.trim();
                if (!trimmed.isEmpty()) {
                    sentences.add(trimmed);
                }
            }
        }
        return sentences;
    }
}
