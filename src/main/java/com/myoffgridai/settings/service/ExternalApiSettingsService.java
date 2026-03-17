package com.myoffgridai.settings.service;

import com.myoffgridai.frontier.FrontierProvider;
import com.myoffgridai.settings.dto.ExternalApiSettingsDto;
import com.myoffgridai.settings.dto.UpdateExternalApiSettingsRequest;
import com.myoffgridai.settings.model.ExternalApiSettings;
import com.myoffgridai.settings.repository.ExternalApiSettingsRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;

/**
 * Manages the singleton {@link ExternalApiSettings} row.
 *
 * <p>Provides read/write access to external API configuration and
 * internal-only methods for decrypting API keys used by downstream
 * services (never exposed via REST).</p>
 */
@Service
public class ExternalApiSettingsService {

    private static final Logger log = LoggerFactory.getLogger(ExternalApiSettingsService.class);

    private final ExternalApiSettingsRepository repository;

    /**
     * Constructs the service.
     *
     * @param repository the external API settings repository
     */
    public ExternalApiSettingsService(ExternalApiSettingsRepository repository) {
        this.repository = repository;
    }

    /**
     * Returns the current external API settings. Creates the singleton row
     * with defaults if it does not yet exist.
     *
     * @return the settings DTO (keys are never included)
     */
    @Transactional
    public ExternalApiSettingsDto getSettings() {
        ExternalApiSettings settings = getOrCreateEntity();
        return toDto(settings);
    }

    /**
     * Updates external API settings from the given request.
     *
     * <p>API keys are only updated when a non-null value is provided.
     * An empty string clears the key. Boolean and enum fields use null
     * to mean "no change".</p>
     *
     * @param request the update request
     * @return the updated settings DTO (keys are never included)
     */
    @Transactional
    public ExternalApiSettingsDto updateSettings(UpdateExternalApiSettingsRequest request) {
        ExternalApiSettings settings = getOrCreateEntity();

        // Only update keys when explicitly provided (non-null)
        if (request.anthropicApiKey() != null) {
            String key = request.anthropicApiKey().isBlank() ? null : request.anthropicApiKey();
            settings.setAnthropicApiKey(key);
            log.info("Anthropic API key {}", key != null ? "updated" : "cleared");
        }
        if (request.braveApiKey() != null) {
            String key = request.braveApiKey().isBlank() ? null : request.braveApiKey();
            settings.setBraveApiKey(key);
            log.info("Brave Search API key {}", key != null ? "updated" : "cleared");
        }

        if (request.huggingFaceToken() != null) {
            String key = request.huggingFaceToken().isBlank() ? null : request.huggingFaceToken();
            settings.setHuggingFaceToken(key);
            log.info("HuggingFace token {}", key != null ? "updated" : "cleared");
        }

        if (request.grokApiKey() != null) {
            String key = request.grokApiKey().isBlank() ? null : request.grokApiKey();
            settings.setGrokApiKey(key);
            log.info("Grok API key {}", key != null ? "updated" : "cleared");
        }

        if (request.openAiApiKey() != null) {
            String key = request.openAiApiKey().isBlank() ? null : request.openAiApiKey();
            settings.setOpenAiApiKey(key);
            log.info("OpenAI API key {}", key != null ? "updated" : "cleared");
        }

        settings.setAnthropicModel(request.anthropicModel());
        settings.setAnthropicEnabled(request.anthropicEnabled());
        settings.setBraveEnabled(request.braveEnabled());
        settings.setHuggingFaceEnabled(request.huggingFaceEnabled());
        settings.setMaxWebFetchSizeKb(request.maxWebFetchSizeKb());
        settings.setSearchResultLimit(request.searchResultLimit());

        if (request.grokEnabled() != null) {
            settings.setGrokEnabled(request.grokEnabled());
        }
        if (request.openAiEnabled() != null) {
            settings.setOpenAiEnabled(request.openAiEnabled());
        }
        if (request.preferredFrontierProvider() != null) {
            settings.setPreferredFrontierProvider(request.preferredFrontierProvider());
        }
        if (request.judgeEnabled() != null) {
            settings.setJudgeEnabled(request.judgeEnabled());
        }
        if (request.judgeModelFilename() != null) {
            settings.setJudgeModelFilename(request.judgeModelFilename().isBlank() ? null : request.judgeModelFilename());
        }
        if (request.judgeScoreThreshold() != null) {
            settings.setJudgeScoreThreshold(request.judgeScoreThreshold());
        }

        settings = repository.save(settings);
        return toDto(settings);
    }

    /**
     * Returns the decrypted Anthropic API key if configured and enabled.
     * For internal service use only — never exposed via REST.
     *
     * @return the decrypted key, or empty if not configured or disabled
     */
    @Transactional(readOnly = true)
    public Optional<String> getAnthropicKey() {
        ExternalApiSettings settings = getOrCreateEntity();
        if (!settings.isAnthropicEnabled() || settings.getAnthropicApiKey() == null) {
            return Optional.empty();
        }
        return Optional.of(settings.getAnthropicApiKey());
    }

    /**
     * Returns the decrypted Brave Search API key if configured and enabled.
     * For internal service use only — never exposed via REST.
     *
     * @return the decrypted key, or empty if not configured or disabled
     */
    @Transactional(readOnly = true)
    public Optional<String> getBraveKey() {
        ExternalApiSettings settings = getOrCreateEntity();
        if (!settings.isBraveEnabled() || settings.getBraveApiKey() == null) {
            return Optional.empty();
        }
        return Optional.of(settings.getBraveApiKey());
    }

    /**
     * Returns the configured Anthropic model name.
     *
     * @return the model string
     */
    @Transactional(readOnly = true)
    public String getAnthropicModel() {
        return getOrCreateEntity().getAnthropicModel();
    }

    /**
     * Returns the max web fetch size in KB.
     *
     * @return the limit in kilobytes
     */
    @Transactional(readOnly = true)
    public int getMaxWebFetchSizeKb() {
        return getOrCreateEntity().getMaxWebFetchSizeKb();
    }

    /**
     * Returns the search result limit.
     *
     * @return the max number of search results
     */
    @Transactional(readOnly = true)
    public int getSearchResultLimit() {
        return getOrCreateEntity().getSearchResultLimit();
    }

    /**
     * Returns the decrypted HuggingFace token if configured and enabled.
     * For internal service use only — never exposed via REST.
     *
     * @return the decrypted token, or empty if not configured or disabled
     */
    @Transactional(readOnly = true)
    public Optional<String> getHuggingFaceToken() {
        ExternalApiSettings settings = getOrCreateEntity();
        if (!settings.isHuggingFaceEnabled() || settings.getHuggingFaceToken() == null) {
            return Optional.empty();
        }
        return Optional.of(settings.getHuggingFaceToken());
    }

    /**
     * Returns the decrypted Grok API key if configured and enabled.
     * For internal service use only — never exposed via REST.
     *
     * @return the decrypted key, or empty if not configured or disabled
     */
    @Transactional(readOnly = true)
    public Optional<String> getGrokKey() {
        ExternalApiSettings settings = getOrCreateEntity();
        if (!settings.isGrokEnabled() || settings.getGrokApiKey() == null) {
            return Optional.empty();
        }
        return Optional.of(settings.getGrokApiKey());
    }

    /**
     * Returns the decrypted OpenAI API key if configured and enabled.
     * For internal service use only — never exposed via REST.
     *
     * @return the decrypted key, or empty if not configured or disabled
     */
    @Transactional(readOnly = true)
    public Optional<String> getOpenAiKey() {
        ExternalApiSettings settings = getOrCreateEntity();
        if (!settings.isOpenAiEnabled() || settings.getOpenAiApiKey() == null) {
            return Optional.empty();
        }
        return Optional.of(settings.getOpenAiApiKey());
    }

    /**
     * Returns the preferred frontier provider for cloud refinement routing.
     *
     * @return the preferred provider enum value
     */
    @Transactional(readOnly = true)
    public FrontierProvider getPreferredFrontierProvider() {
        FrontierProvider provider = getOrCreateEntity().getPreferredFrontierProvider();
        return provider != null ? provider : FrontierProvider.CLAUDE;
    }

    private ExternalApiSettings getOrCreateEntity() {
        return repository.findBySingletonGuard("SINGLETON")
                .orElseGet(() -> {
                    log.info("Creating default ExternalApiSettings singleton");
                    return repository.save(new ExternalApiSettings());
                });
    }

    private ExternalApiSettingsDto toDto(ExternalApiSettings entity) {
        return new ExternalApiSettingsDto(
                entity.isAnthropicEnabled(),
                entity.getAnthropicModel(),
                entity.getAnthropicApiKey() != null,
                entity.isBraveEnabled(),
                entity.getBraveApiKey() != null,
                entity.getMaxWebFetchSizeKb(),
                entity.getSearchResultLimit(),
                entity.isHuggingFaceEnabled(),
                entity.getHuggingFaceToken() != null,
                entity.isGrokEnabled(),
                entity.getGrokApiKey() != null,
                entity.isOpenAiEnabled(),
                entity.getOpenAiApiKey() != null,
                entity.getPreferredFrontierProvider(),
                entity.isJudgeEnabled(),
                entity.getJudgeModelFilename(),
                entity.getJudgeScoreThreshold()
        );
    }
}
