package com.myoffgridai.frontier;

import com.myoffgridai.settings.service.ExternalApiSettingsService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

/**
 * Routes frontier completion requests to the best available provider.
 *
 * <p>Respects the user's preferred provider from settings. If the preferred
 * provider is unavailable, falls back through the priority chain:
 * {@link FrontierProvider#CLAUDE} → {@link FrontierProvider#GROK} →
 * {@link FrontierProvider#OPENAI}.</p>
 */
@Service
public class FrontierApiRouter {

    private static final Logger log = LoggerFactory.getLogger(FrontierApiRouter.class);

    private static final List<FrontierProvider> FALLBACK_ORDER = List.of(
            FrontierProvider.CLAUDE, FrontierProvider.GROK, FrontierProvider.OPENAI
    );

    private final List<FrontierApiClient> clients;
    private final ExternalApiSettingsService settingsService;

    /**
     * Constructs the frontier API router.
     *
     * @param clients         all registered frontier API clients (auto-collected by Spring)
     * @param settingsService external API settings for preferred provider lookup
     */
    public FrontierApiRouter(List<FrontierApiClient> clients,
                              ExternalApiSettingsService settingsService) {
        this.clients = clients;
        this.settingsService = settingsService;
    }

    /**
     * Sends a completion request to the preferred provider, falling back to
     * the next available provider in priority order.
     *
     * @param systemPrompt the system prompt
     * @param userMessage  the user message
     * @return the enhanced response, or empty if no provider is available
     */
    public Optional<String> complete(String systemPrompt, String userMessage) {
        FrontierProvider preferred = settingsService.getPreferredFrontierProvider();

        // Try preferred provider first
        Optional<FrontierApiClient> preferredClient = findClient(preferred);
        if (preferredClient.isPresent() && preferredClient.get().isAvailable()) {
            log.info("Using preferred frontier provider: {}", preferred);
            Optional<String> result = preferredClient.get().complete(systemPrompt, userMessage);
            if (result.isPresent()) {
                return result;
            }
            log.warn("Preferred frontier provider {} failed — trying fallbacks", preferred);
        }

        // Fallback through priority chain
        for (FrontierProvider provider : FALLBACK_ORDER) {
            if (provider == preferred) {
                continue;
            }
            Optional<FrontierApiClient> client = findClient(provider);
            if (client.isPresent() && client.get().isAvailable()) {
                log.info("Falling back to frontier provider: {}", provider);
                Optional<String> result = client.get().complete(systemPrompt, userMessage);
                if (result.isPresent()) {
                    return result;
                }
            }
        }

        log.warn("No frontier provider available or all failed");
        return Optional.empty();
    }

    /**
     * Returns whether at least one frontier provider is available.
     *
     * @return true if any provider has a key configured and is enabled
     */
    public boolean isAnyAvailable() {
        return clients.stream().anyMatch(FrontierApiClient::isAvailable);
    }

    /**
     * Returns the list of currently available frontier providers.
     *
     * @return a list of available provider enums
     */
    public List<FrontierProvider> getAvailableProviders() {
        return clients.stream()
                .filter(FrontierApiClient::isAvailable)
                .map(FrontierApiClient::getProvider)
                .toList();
    }

    private Optional<FrontierApiClient> findClient(FrontierProvider provider) {
        return clients.stream()
                .filter(c -> c.getProvider() == provider)
                .findFirst();
    }
}
