package com.myoffgridai.frontier;

import java.util.Optional;

/**
 * Contract for cloud frontier API providers that supply enhanced responses
 * when the local model's output is deemed insufficient by the AI judge.
 *
 * <p>Each implementation handles a single {@link FrontierProvider} and must
 * degrade gracefully — returning {@link Optional#empty()} on any failure.</p>
 */
public interface FrontierApiClient {

    /**
     * Returns the provider this client handles.
     *
     * @return the frontier provider enum value
     */
    FrontierProvider getProvider();

    /**
     * Returns true if this provider has an API key configured and is enabled.
     *
     * @return true if the provider is ready to accept requests
     */
    boolean isAvailable();

    /**
     * Sends a single-turn completion request to the provider.
     *
     * @param systemPrompt the system prompt
     * @param userMessage  the user message
     * @return the response text, or {@link Optional#empty()} on any failure
     */
    Optional<String> complete(String systemPrompt, String userMessage);
}
