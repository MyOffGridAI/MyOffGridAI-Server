package com.myoffgridai.frontier;

import com.myoffgridai.settings.service.ExternalApiSettingsService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

/**
 * Unit tests for {@link FrontierApiRouter}.
 *
 * <p>Validates preferred-provider routing, fallback ordering, and
 * availability checks across all three frontier providers.</p>
 */
@ExtendWith(MockitoExtension.class)
class FrontierApiRouterTest {

    @Mock private FrontierApiClient claudeClient;
    @Mock private FrontierApiClient grokClient;
    @Mock private FrontierApiClient openAiClient;
    @Mock private ExternalApiSettingsService settingsService;

    private FrontierApiRouter router;

    @BeforeEach
    void setUp() {
        lenient().when(claudeClient.getProvider()).thenReturn(FrontierProvider.CLAUDE);
        lenient().when(grokClient.getProvider()).thenReturn(FrontierProvider.GROK);
        lenient().when(openAiClient.getProvider()).thenReturn(FrontierProvider.OPENAI);

        router = new FrontierApiRouter(
                List.of(claudeClient, grokClient, openAiClient), settingsService);
    }

    // ── complete tests ──────────────────────────────────────────────────────

    @Test
    void complete_usesPreferredProvider() {
        when(settingsService.getPreferredFrontierProvider()).thenReturn(FrontierProvider.CLAUDE);
        when(claudeClient.isAvailable()).thenReturn(true);
        when(claudeClient.complete(anyString(), anyString())).thenReturn(Optional.of("Claude response"));

        Optional<String> result = router.complete("system", "user");

        assertTrue(result.isPresent());
        assertEquals("Claude response", result.get());
        verify(grokClient, never()).complete(anyString(), anyString());
        verify(openAiClient, never()).complete(anyString(), anyString());
    }

    @Test
    void complete_fallsBackWhenPreferredUnavailable() {
        when(settingsService.getPreferredFrontierProvider()).thenReturn(FrontierProvider.CLAUDE);
        when(claudeClient.isAvailable()).thenReturn(false);
        when(grokClient.isAvailable()).thenReturn(true);
        when(grokClient.complete(anyString(), anyString())).thenReturn(Optional.of("Grok response"));

        Optional<String> result = router.complete("system", "user");

        assertTrue(result.isPresent());
        assertEquals("Grok response", result.get());
    }

    @Test
    void complete_fallsBackWhenPreferredFails() {
        when(settingsService.getPreferredFrontierProvider()).thenReturn(FrontierProvider.CLAUDE);
        when(claudeClient.isAvailable()).thenReturn(true);
        when(claudeClient.complete(anyString(), anyString())).thenReturn(Optional.empty());
        when(grokClient.isAvailable()).thenReturn(false);
        when(openAiClient.isAvailable()).thenReturn(true);
        when(openAiClient.complete(anyString(), anyString())).thenReturn(Optional.of("OpenAI response"));

        Optional<String> result = router.complete("system", "user");

        assertTrue(result.isPresent());
        assertEquals("OpenAI response", result.get());
    }

    @Test
    void complete_returnsEmptyWhenAllFail() {
        when(settingsService.getPreferredFrontierProvider()).thenReturn(FrontierProvider.CLAUDE);
        when(claudeClient.isAvailable()).thenReturn(false);
        when(grokClient.isAvailable()).thenReturn(false);
        when(openAiClient.isAvailable()).thenReturn(false);

        Optional<String> result = router.complete("system", "user");

        assertTrue(result.isEmpty());
    }

    @Test
    void complete_skipsPreferredInFallbackChain() {
        when(settingsService.getPreferredFrontierProvider()).thenReturn(FrontierProvider.GROK);
        when(grokClient.isAvailable()).thenReturn(true);
        when(grokClient.complete(anyString(), anyString())).thenReturn(Optional.empty());
        // Grok fails, fall back to CLAUDE (first in chain), then OPENAI
        when(claudeClient.isAvailable()).thenReturn(true);
        when(claudeClient.complete(anyString(), anyString())).thenReturn(Optional.of("Claude fallback"));

        Optional<String> result = router.complete("system", "user");

        assertTrue(result.isPresent());
        assertEquals("Claude fallback", result.get());
        // GROK was already tried as preferred, so only CLAUDE from fallback
        verify(openAiClient, never()).complete(anyString(), anyString());
    }

    @Test
    void complete_respectsGrokAsPreferred() {
        when(settingsService.getPreferredFrontierProvider()).thenReturn(FrontierProvider.GROK);
        when(grokClient.isAvailable()).thenReturn(true);
        when(grokClient.complete(anyString(), anyString())).thenReturn(Optional.of("Grok preferred"));

        Optional<String> result = router.complete("system", "user");

        assertTrue(result.isPresent());
        assertEquals("Grok preferred", result.get());
        verify(claudeClient, never()).complete(anyString(), anyString());
        verify(openAiClient, never()).complete(anyString(), anyString());
    }

    // ── isAnyAvailable tests ────────────────────────────────────────────────

    @Test
    void isAnyAvailable_trueWhenOneAvailable() {
        when(claudeClient.isAvailable()).thenReturn(false);
        when(grokClient.isAvailable()).thenReturn(true);

        assertTrue(router.isAnyAvailable());
    }

    @Test
    void isAnyAvailable_falseWhenNoneAvailable() {
        when(claudeClient.isAvailable()).thenReturn(false);
        when(grokClient.isAvailable()).thenReturn(false);
        when(openAiClient.isAvailable()).thenReturn(false);

        assertFalse(router.isAnyAvailable());
    }

    // ── getAvailableProviders tests ─────────────────────────────────────────

    @Test
    void getAvailableProviders_returnsOnlyAvailable() {
        when(claudeClient.isAvailable()).thenReturn(true);
        when(grokClient.isAvailable()).thenReturn(false);
        when(openAiClient.isAvailable()).thenReturn(true);

        List<FrontierProvider> available = router.getAvailableProviders();

        assertEquals(2, available.size());
        assertTrue(available.contains(FrontierProvider.CLAUDE));
        assertTrue(available.contains(FrontierProvider.OPENAI));
        assertFalse(available.contains(FrontierProvider.GROK));
    }

    @Test
    void getAvailableProviders_returnsEmptyWhenNoneAvailable() {
        when(claudeClient.isAvailable()).thenReturn(false);
        when(grokClient.isAvailable()).thenReturn(false);
        when(openAiClient.isAvailable()).thenReturn(false);

        assertTrue(router.getAvailableProviders().isEmpty());
    }
}
