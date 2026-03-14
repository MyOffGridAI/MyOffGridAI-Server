package com.myoffgridai.common.util;

import com.myoffgridai.ai.dto.OllamaMessage;

import java.util.ArrayList;
import java.util.List;

/**
 * Utility class for estimating token counts and managing context window limits.
 *
 * <p>Uses a character-based approximation (1 token ~= 4 characters) for context
 * window management. This is not intended for billing accuracy.</p>
 */
public final class TokenCounter {

    private static final int CHARS_PER_TOKEN = 4;

    private TokenCounter() {
        // Prevent instantiation
    }

    /**
     * Estimates the token count of a text string.
     *
     * @param text the text to estimate
     * @return the estimated number of tokens
     */
    public static int estimateTokens(String text) {
        if (text == null || text.isEmpty()) {
            return 0;
        }
        return Math.max(1, (text.length() + CHARS_PER_TOKEN - 1) / CHARS_PER_TOKEN);
    }

    /**
     * Truncates a message list by removing oldest non-system messages until
     * the estimated total is under the max token limit.
     *
     * <p>Always preserves the SYSTEM message (first) and the most recent
     * USER message (last).</p>
     *
     * @param messages  the ordered message list
     * @param maxTokens the maximum allowed token count
     * @return a truncated list fitting within the token limit
     */
    public static List<OllamaMessage> truncateToTokenLimit(List<OllamaMessage> messages, int maxTokens) {
        if (messages == null || messages.isEmpty()) {
            return List.of();
        }

        int totalTokens = messages.stream()
                .mapToInt(m -> estimateTokens(m.content()))
                .sum();

        if (totalTokens <= maxTokens) {
            return new ArrayList<>(messages);
        }

        // Always keep system message (index 0) and latest user message (last)
        List<OllamaMessage> result = new ArrayList<>(messages);

        // Remove oldest non-system, non-last messages until under limit
        while (estimateTotalTokens(result) > maxTokens && result.size() > 2) {
            // Remove the second element (oldest non-system message)
            result.remove(1);
        }

        return result;
    }

    private static int estimateTotalTokens(List<OllamaMessage> messages) {
        return messages.stream()
                .mapToInt(m -> estimateTokens(m.content()))
                .sum();
    }
}
