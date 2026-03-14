package com.myoffgridai.common.util;

import com.myoffgridai.ai.dto.OllamaMessage;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class TokenCounterTest {

    @Test
    void estimateTokens_emptyString_returnsZero() {
        assertEquals(0, TokenCounter.estimateTokens(""));
    }

    @Test
    void estimateTokens_nullString_returnsZero() {
        assertEquals(0, TokenCounter.estimateTokens(null));
    }

    @Test
    void estimateTokens_singleWord_returnsAtLeastOne() {
        int tokens = TokenCounter.estimateTokens("hello");
        assertTrue(tokens >= 1);
    }

    @Test
    void estimateTokens_longText_approximatesFourCharsPerToken() {
        String text = "a".repeat(400);
        int tokens = TokenCounter.estimateTokens(text);
        assertEquals(100, tokens);
    }

    @Test
    void truncateToTokenLimit_noTruncationNeeded_returnsAll() {
        List<OllamaMessage> messages = List.of(
                new OllamaMessage("system", "system prompt"),
                new OllamaMessage("user", "hello")
        );
        List<OllamaMessage> result = TokenCounter.truncateToTokenLimit(messages, 10000);
        assertEquals(2, result.size());
    }

    @Test
    void truncateToTokenLimit_truncation_preservesSystemAndLatestUser() {
        List<OllamaMessage> messages = new ArrayList<>();
        messages.add(new OllamaMessage("system", "system prompt"));
        messages.add(new OllamaMessage("user", "a".repeat(4000)));
        messages.add(new OllamaMessage("assistant", "b".repeat(4000)));
        messages.add(new OllamaMessage("user", "c".repeat(4000)));

        List<OllamaMessage> result = TokenCounter.truncateToTokenLimit(messages, 2500);

        // Should keep system (first) and latest user (last)
        assertTrue(result.size() >= 2);
        assertEquals("system", result.get(0).role());
        assertEquals("user", result.get(result.size() - 1).role());
        assertEquals("c".repeat(4000), result.get(result.size() - 1).content());
    }

    @Test
    void truncateToTokenLimit_emptyList_returnsEmpty() {
        List<OllamaMessage> result = TokenCounter.truncateToTokenLimit(List.of(), 1000);
        assertTrue(result.isEmpty());
    }

    @Test
    void truncateToTokenLimit_nullList_returnsEmpty() {
        List<OllamaMessage> result = TokenCounter.truncateToTokenLimit(null, 1000);
        assertTrue(result.isEmpty());
    }
}
