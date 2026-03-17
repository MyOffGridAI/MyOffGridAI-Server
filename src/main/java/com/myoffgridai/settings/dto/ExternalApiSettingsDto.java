package com.myoffgridai.settings.dto;

import com.myoffgridai.frontier.FrontierProvider;

/**
 * Response DTO for external API settings. Never contains actual key values.
 *
 * @param anthropicEnabled           whether the Anthropic API is enabled
 * @param anthropicModel             the configured Anthropic model
 * @param anthropicKeyConfigured     whether an Anthropic API key has been set
 * @param braveEnabled               whether the Brave Search API is enabled
 * @param braveKeyConfigured         whether a Brave Search API key has been set
 * @param maxWebFetchSizeKb          max content size per URL fetch in KB
 * @param searchResultLimit          max results per search query
 * @param huggingFaceEnabled         whether HuggingFace access is enabled
 * @param huggingFaceKeyConfigured   whether a HuggingFace token has been set
 * @param grokEnabled                whether the Grok (xAI) frontier provider is enabled
 * @param grokKeyConfigured          whether a Grok API key has been set
 * @param openAiEnabled              whether the OpenAI frontier provider is enabled
 * @param openAiKeyConfigured        whether an OpenAI API key has been set
 * @param preferredFrontierProvider  the preferred cloud frontier provider for routing
 * @param judgeEnabled               whether the AI judge evaluation pipeline is enabled
 * @param judgeModelFilename         the configured judge model filename (nullable)
 * @param judgeScoreThreshold        the minimum score below which cloud refinement is triggered
 */
public record ExternalApiSettingsDto(
        boolean anthropicEnabled,
        String anthropicModel,
        boolean anthropicKeyConfigured,
        boolean braveEnabled,
        boolean braveKeyConfigured,
        int maxWebFetchSizeKb,
        int searchResultLimit,
        boolean huggingFaceEnabled,
        boolean huggingFaceKeyConfigured,
        boolean grokEnabled,
        boolean grokKeyConfigured,
        boolean openAiEnabled,
        boolean openAiKeyConfigured,
        FrontierProvider preferredFrontierProvider,
        boolean judgeEnabled,
        String judgeModelFilename,
        double judgeScoreThreshold
) {
}
