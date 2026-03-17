package com.myoffgridai.frontier;

/**
 * Enumerates the available cloud frontier API providers for response enhancement.
 *
 * <p>When the AI judge determines that a local model response is insufficient,
 * the system routes to one of these providers for a higher-quality cloud response.
 * Providers are tried in priority order: {@link #CLAUDE} → {@link #GROK} → {@link #OPENAI}.</p>
 */
public enum FrontierProvider {

    /** Anthropic Claude — highest priority frontier provider. */
    CLAUDE,

    /** xAI Grok — second priority, uses OpenAI-compatible API. */
    GROK,

    /** OpenAI GPT — third priority fallback provider. */
    OPENAI
}
