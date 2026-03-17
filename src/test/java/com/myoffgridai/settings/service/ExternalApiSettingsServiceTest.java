package com.myoffgridai.settings.service;

import com.myoffgridai.frontier.FrontierProvider;
import com.myoffgridai.settings.dto.ExternalApiSettingsDto;
import com.myoffgridai.settings.dto.UpdateExternalApiSettingsRequest;
import com.myoffgridai.settings.model.ExternalApiSettings;
import com.myoffgridai.settings.repository.ExternalApiSettingsRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/**
 * Tests for {@link ExternalApiSettingsService}.
 */
@ExtendWith(MockitoExtension.class)
class ExternalApiSettingsServiceTest {

    @Mock
    private ExternalApiSettingsRepository repository;

    @InjectMocks
    private ExternalApiSettingsService service;

    private ExternalApiSettings entity;

    @BeforeEach
    void setUp() {
        entity = new ExternalApiSettings();
        entity.setAnthropicModel("claude-sonnet-4-20250514");
    }

    // ── Helper to build a full request with sensible defaults ───────────

    private UpdateExternalApiSettingsRequest buildRequest(
            String anthropicApiKey, String anthropicModel, boolean anthropicEnabled,
            String braveApiKey, boolean braveEnabled, int maxWebFetchSizeKb, int searchResultLimit,
            String huggingFaceToken, boolean huggingFaceEnabled) {
        return new UpdateExternalApiSettingsRequest(
                anthropicApiKey, anthropicModel, anthropicEnabled,
                braveApiKey, braveEnabled, maxWebFetchSizeKb, searchResultLimit,
                huggingFaceToken, huggingFaceEnabled,
                null, null, null, null, null, null, null, null
        );
    }

    @Test
    void getSettings_createsDefaultsWhenNotFound() {
        when(repository.findBySingletonGuard("SINGLETON")).thenReturn(Optional.empty());
        when(repository.save(any())).thenReturn(entity);

        ExternalApiSettingsDto dto = service.getSettings();

        assertFalse(dto.anthropicEnabled());
        assertFalse(dto.braveEnabled());
        assertFalse(dto.anthropicKeyConfigured());
        assertFalse(dto.braveKeyConfigured());
        assertEquals(512, dto.maxWebFetchSizeKb());
        assertEquals(5, dto.searchResultLimit());
        assertFalse(dto.grokEnabled());
        assertFalse(dto.grokKeyConfigured());
        assertFalse(dto.openAiEnabled());
        assertFalse(dto.openAiKeyConfigured());
        assertFalse(dto.judgeEnabled());
        assertNull(dto.judgeModelFilename());
        assertEquals(7.5, dto.judgeScoreThreshold());
        verify(repository).save(any());
    }

    @Test
    void getSettings_returnsExistingEntity() {
        entity.setAnthropicEnabled(true);
        entity.setAnthropicApiKey("encrypted-key");
        when(repository.findBySingletonGuard("SINGLETON")).thenReturn(Optional.of(entity));

        ExternalApiSettingsDto dto = service.getSettings();

        assertTrue(dto.anthropicEnabled());
        assertTrue(dto.anthropicKeyConfigured());
    }

    @Test
    void getSettings_neverReturnsActualKeys() {
        entity.setAnthropicApiKey("secret-key-123");
        entity.setBraveApiKey("brave-key-456");
        entity.setGrokApiKey("grok-key-789");
        entity.setOpenAiApiKey("openai-key-012");
        when(repository.findBySingletonGuard("SINGLETON")).thenReturn(Optional.of(entity));

        ExternalApiSettingsDto dto = service.getSettings();

        assertTrue(dto.anthropicKeyConfigured());
        assertTrue(dto.braveKeyConfigured());
        assertTrue(dto.grokKeyConfigured());
        assertTrue(dto.openAiKeyConfigured());
    }

    @Test
    void updateSettings_updatesKeyWhenProvided() {
        when(repository.findBySingletonGuard("SINGLETON")).thenReturn(Optional.of(entity));
        when(repository.save(any())).thenReturn(entity);

        UpdateExternalApiSettingsRequest request = buildRequest(
                "new-anthropic-key", "claude-sonnet-4-20250514", true,
                "new-brave-key", true, 1024, 10,
                null, false
        );

        service.updateSettings(request);

        ArgumentCaptor<ExternalApiSettings> captor = ArgumentCaptor.forClass(ExternalApiSettings.class);
        verify(repository).save(captor.capture());

        ExternalApiSettings saved = captor.getValue();
        assertEquals("new-anthropic-key", saved.getAnthropicApiKey());
        assertEquals("new-brave-key", saved.getBraveApiKey());
        assertTrue(saved.isAnthropicEnabled());
        assertTrue(saved.isBraveEnabled());
        assertEquals(1024, saved.getMaxWebFetchSizeKb());
        assertEquals(10, saved.getSearchResultLimit());
    }

    @Test
    void updateSettings_doesNotChangeKeyWhenNull() {
        entity.setAnthropicApiKey("existing-key");
        entity.setBraveApiKey("existing-brave-key");
        when(repository.findBySingletonGuard("SINGLETON")).thenReturn(Optional.of(entity));
        when(repository.save(any())).thenReturn(entity);

        UpdateExternalApiSettingsRequest request = buildRequest(
                null, "claude-sonnet-4-20250514", true,
                null, true, 512, 5,
                null, false
        );

        service.updateSettings(request);

        ArgumentCaptor<ExternalApiSettings> captor = ArgumentCaptor.forClass(ExternalApiSettings.class);
        verify(repository).save(captor.capture());

        assertEquals("existing-key", captor.getValue().getAnthropicApiKey());
        assertEquals("existing-brave-key", captor.getValue().getBraveApiKey());
    }

    @Test
    void updateSettings_clearsKeyOnEmptyString() {
        entity.setAnthropicApiKey("existing-key");
        when(repository.findBySingletonGuard("SINGLETON")).thenReturn(Optional.of(entity));
        when(repository.save(any())).thenReturn(entity);

        UpdateExternalApiSettingsRequest request = buildRequest(
                "", "claude-sonnet-4-20250514", false,
                null, false, 512, 5,
                null, false
        );

        service.updateSettings(request);

        ArgumentCaptor<ExternalApiSettings> captor = ArgumentCaptor.forClass(ExternalApiSettings.class);
        verify(repository).save(captor.capture());

        assertNull(captor.getValue().getAnthropicApiKey());
    }

    @Test
    void getAnthropicKey_returnsEmptyWhenDisabled() {
        entity.setAnthropicApiKey("key");
        entity.setAnthropicEnabled(false);
        when(repository.findBySingletonGuard("SINGLETON")).thenReturn(Optional.of(entity));

        assertTrue(service.getAnthropicKey().isEmpty());
    }

    @Test
    void getAnthropicKey_returnsEmptyWhenNoKey() {
        entity.setAnthropicEnabled(true);
        entity.setAnthropicApiKey(null);
        when(repository.findBySingletonGuard("SINGLETON")).thenReturn(Optional.of(entity));

        assertTrue(service.getAnthropicKey().isEmpty());
    }

    @Test
    void getAnthropicKey_returnsKeyWhenConfigured() {
        entity.setAnthropicApiKey("my-key");
        entity.setAnthropicEnabled(true);
        when(repository.findBySingletonGuard("SINGLETON")).thenReturn(Optional.of(entity));

        Optional<String> key = service.getAnthropicKey();
        assertTrue(key.isPresent());
        assertEquals("my-key", key.get());
    }

    @Test
    void getBraveKey_returnsEmptyWhenDisabled() {
        entity.setBraveApiKey("key");
        entity.setBraveEnabled(false);
        when(repository.findBySingletonGuard("SINGLETON")).thenReturn(Optional.of(entity));

        assertTrue(service.getBraveKey().isEmpty());
    }

    @Test
    void getBraveKey_returnsKeyWhenConfigured() {
        entity.setBraveApiKey("brave-key");
        entity.setBraveEnabled(true);
        when(repository.findBySingletonGuard("SINGLETON")).thenReturn(Optional.of(entity));

        Optional<String> key = service.getBraveKey();
        assertTrue(key.isPresent());
        assertEquals("brave-key", key.get());
    }

    // ── HuggingFace token tests ─────────────────────────────────────────────

    @Test
    void getHuggingFaceToken_returnsEmptyWhenDisabled() {
        entity.setHuggingFaceToken("hf_token_123");
        entity.setHuggingFaceEnabled(false);
        when(repository.findBySingletonGuard("SINGLETON")).thenReturn(Optional.of(entity));

        assertTrue(service.getHuggingFaceToken().isEmpty());
    }

    @Test
    void getHuggingFaceToken_returnsEmptyWhenNoToken() {
        entity.setHuggingFaceEnabled(true);
        entity.setHuggingFaceToken(null);
        when(repository.findBySingletonGuard("SINGLETON")).thenReturn(Optional.of(entity));

        assertTrue(service.getHuggingFaceToken().isEmpty());
    }

    @Test
    void getHuggingFaceToken_returnsTokenWhenEnabledAndSet() {
        entity.setHuggingFaceToken("hf_my_token");
        entity.setHuggingFaceEnabled(true);
        when(repository.findBySingletonGuard("SINGLETON")).thenReturn(Optional.of(entity));

        Optional<String> token = service.getHuggingFaceToken();
        assertTrue(token.isPresent());
        assertEquals("hf_my_token", token.get());
    }

    @Test
    void updateSettings_handlesHuggingFaceTokenUpdate() {
        when(repository.findBySingletonGuard("SINGLETON")).thenReturn(Optional.of(entity));
        when(repository.save(any())).thenReturn(entity);

        UpdateExternalApiSettingsRequest request = buildRequest(
                null, "claude-sonnet-4-20250514", false,
                null, false, 512, 5,
                "hf_new_token_456", true
        );

        service.updateSettings(request);

        ArgumentCaptor<ExternalApiSettings> captor = ArgumentCaptor.forClass(ExternalApiSettings.class);
        verify(repository).save(captor.capture());

        ExternalApiSettings saved = captor.getValue();
        assertEquals("hf_new_token_456", saved.getHuggingFaceToken());
        assertTrue(saved.isHuggingFaceEnabled());
    }

    @Test
    void updateSettings_clearsHuggingFaceTokenOnEmptyString() {
        entity.setHuggingFaceToken("existing-hf-token");
        when(repository.findBySingletonGuard("SINGLETON")).thenReturn(Optional.of(entity));
        when(repository.save(any())).thenReturn(entity);

        UpdateExternalApiSettingsRequest request = buildRequest(
                null, "claude-sonnet-4-20250514", false,
                null, false, 512, 5,
                "", false
        );

        service.updateSettings(request);

        ArgumentCaptor<ExternalApiSettings> captor = ArgumentCaptor.forClass(ExternalApiSettings.class);
        verify(repository).save(captor.capture());

        assertNull(captor.getValue().getHuggingFaceToken());
    }

    @Test
    void updateSettings_doesNotChangeHuggingFaceTokenWhenNull() {
        entity.setHuggingFaceToken("existing-hf-token");
        when(repository.findBySingletonGuard("SINGLETON")).thenReturn(Optional.of(entity));
        when(repository.save(any())).thenReturn(entity);

        UpdateExternalApiSettingsRequest request = buildRequest(
                null, "claude-sonnet-4-20250514", false,
                null, false, 512, 5,
                null, true
        );

        service.updateSettings(request);

        ArgumentCaptor<ExternalApiSettings> captor = ArgumentCaptor.forClass(ExternalApiSettings.class);
        verify(repository).save(captor.capture());

        assertEquals("existing-hf-token", captor.getValue().getHuggingFaceToken());
    }

    @Test
    void getSettings_defaultsIncludeHuggingFaceFields() {
        when(repository.findBySingletonGuard("SINGLETON")).thenReturn(Optional.empty());
        when(repository.save(any())).thenReturn(entity);

        ExternalApiSettingsDto dto = service.getSettings();

        assertFalse(dto.huggingFaceEnabled());
        assertFalse(dto.huggingFaceKeyConfigured());
    }

    @Test
    void toDto_includesHuggingFaceKeyConfiguredCorrectly() {
        entity.setHuggingFaceEnabled(true);
        entity.setHuggingFaceToken("hf_secret_token");
        when(repository.findBySingletonGuard("SINGLETON")).thenReturn(Optional.of(entity));

        ExternalApiSettingsDto dto = service.getSettings();

        assertTrue(dto.huggingFaceEnabled());
        assertTrue(dto.huggingFaceKeyConfigured());
    }

    @Test
    void toDto_huggingFaceKeyConfiguredFalseWhenNoToken() {
        entity.setHuggingFaceEnabled(true);
        entity.setHuggingFaceToken(null);
        when(repository.findBySingletonGuard("SINGLETON")).thenReturn(Optional.of(entity));

        ExternalApiSettingsDto dto = service.getSettings();

        assertTrue(dto.huggingFaceEnabled());
        assertFalse(dto.huggingFaceKeyConfigured());
    }

    // ── Grok key tests ──────────────────────────────────────────────────────

    @Test
    void getGrokKey_returnsEmptyWhenDisabled() {
        entity.setGrokApiKey("grok-key");
        entity.setGrokEnabled(false);
        when(repository.findBySingletonGuard("SINGLETON")).thenReturn(Optional.of(entity));

        assertTrue(service.getGrokKey().isEmpty());
    }

    @Test
    void getGrokKey_returnsEmptyWhenNoKey() {
        entity.setGrokEnabled(true);
        entity.setGrokApiKey(null);
        when(repository.findBySingletonGuard("SINGLETON")).thenReturn(Optional.of(entity));

        assertTrue(service.getGrokKey().isEmpty());
    }

    @Test
    void getGrokKey_returnsKeyWhenConfigured() {
        entity.setGrokApiKey("grok-key-123");
        entity.setGrokEnabled(true);
        when(repository.findBySingletonGuard("SINGLETON")).thenReturn(Optional.of(entity));

        Optional<String> key = service.getGrokKey();
        assertTrue(key.isPresent());
        assertEquals("grok-key-123", key.get());
    }

    // ── OpenAI key tests ────────────────────────────────────────────────────

    @Test
    void getOpenAiKey_returnsEmptyWhenDisabled() {
        entity.setOpenAiApiKey("openai-key");
        entity.setOpenAiEnabled(false);
        when(repository.findBySingletonGuard("SINGLETON")).thenReturn(Optional.of(entity));

        assertTrue(service.getOpenAiKey().isEmpty());
    }

    @Test
    void getOpenAiKey_returnsEmptyWhenNoKey() {
        entity.setOpenAiEnabled(true);
        entity.setOpenAiApiKey(null);
        when(repository.findBySingletonGuard("SINGLETON")).thenReturn(Optional.of(entity));

        assertTrue(service.getOpenAiKey().isEmpty());
    }

    @Test
    void getOpenAiKey_returnsKeyWhenConfigured() {
        entity.setOpenAiApiKey("sk-openai-key");
        entity.setOpenAiEnabled(true);
        when(repository.findBySingletonGuard("SINGLETON")).thenReturn(Optional.of(entity));

        Optional<String> key = service.getOpenAiKey();
        assertTrue(key.isPresent());
        assertEquals("sk-openai-key", key.get());
    }

    // ── Frontier provider tests ─────────────────────────────────────────────

    @Test
    void getPreferredFrontierProvider_defaultsToClaude() {
        when(repository.findBySingletonGuard("SINGLETON")).thenReturn(Optional.of(entity));

        assertEquals(FrontierProvider.CLAUDE, service.getPreferredFrontierProvider());
    }

    @Test
    void getPreferredFrontierProvider_returnsConfiguredProvider() {
        entity.setPreferredFrontierProvider(FrontierProvider.GROK);
        when(repository.findBySingletonGuard("SINGLETON")).thenReturn(Optional.of(entity));

        assertEquals(FrontierProvider.GROK, service.getPreferredFrontierProvider());
    }

    // ── Judge settings tests ────────────────────────────────────────────────

    @Test
    void updateSettings_updatesGrokAndOpenAiKeys() {
        when(repository.findBySingletonGuard("SINGLETON")).thenReturn(Optional.of(entity));
        when(repository.save(any())).thenReturn(entity);

        UpdateExternalApiSettingsRequest request = new UpdateExternalApiSettingsRequest(
                null, "claude-sonnet-4-20250514", false,
                null, false, 512, 5,
                null, false,
                "new-grok-key", true,
                "new-openai-key", true,
                FrontierProvider.GROK,
                null, null, null
        );

        service.updateSettings(request);

        ArgumentCaptor<ExternalApiSettings> captor = ArgumentCaptor.forClass(ExternalApiSettings.class);
        verify(repository).save(captor.capture());

        ExternalApiSettings saved = captor.getValue();
        assertEquals("new-grok-key", saved.getGrokApiKey());
        assertTrue(saved.isGrokEnabled());
        assertEquals("new-openai-key", saved.getOpenAiApiKey());
        assertTrue(saved.isOpenAiEnabled());
        assertEquals(FrontierProvider.GROK, saved.getPreferredFrontierProvider());
    }

    @Test
    void updateSettings_clearsGrokKeyOnEmptyString() {
        entity.setGrokApiKey("existing-grok-key");
        when(repository.findBySingletonGuard("SINGLETON")).thenReturn(Optional.of(entity));
        when(repository.save(any())).thenReturn(entity);

        UpdateExternalApiSettingsRequest request = new UpdateExternalApiSettingsRequest(
                null, "claude-sonnet-4-20250514", false,
                null, false, 512, 5,
                null, false,
                "", null, null, null, null, null, null, null
        );

        service.updateSettings(request);

        ArgumentCaptor<ExternalApiSettings> captor = ArgumentCaptor.forClass(ExternalApiSettings.class);
        verify(repository).save(captor.capture());
        assertNull(captor.getValue().getGrokApiKey());
    }

    @Test
    void updateSettings_updatesJudgeSettings() {
        when(repository.findBySingletonGuard("SINGLETON")).thenReturn(Optional.of(entity));
        when(repository.save(any())).thenReturn(entity);

        UpdateExternalApiSettingsRequest request = new UpdateExternalApiSettingsRequest(
                null, "claude-sonnet-4-20250514", false,
                null, false, 512, 5,
                null, false,
                null, null, null, null, null,
                true, "judge-model.gguf", 8.0
        );

        service.updateSettings(request);

        ArgumentCaptor<ExternalApiSettings> captor = ArgumentCaptor.forClass(ExternalApiSettings.class);
        verify(repository).save(captor.capture());

        ExternalApiSettings saved = captor.getValue();
        assertTrue(saved.isJudgeEnabled());
        assertEquals("judge-model.gguf", saved.getJudgeModelFilename());
        assertEquals(8.0, saved.getJudgeScoreThreshold());
    }

    @Test
    void updateSettings_clearsJudgeModelFilenameOnEmptyString() {
        entity.setJudgeModelFilename("old-judge.gguf");
        when(repository.findBySingletonGuard("SINGLETON")).thenReturn(Optional.of(entity));
        when(repository.save(any())).thenReturn(entity);

        UpdateExternalApiSettingsRequest request = new UpdateExternalApiSettingsRequest(
                null, "claude-sonnet-4-20250514", false,
                null, false, 512, 5,
                null, false,
                null, null, null, null, null,
                null, "", null
        );

        service.updateSettings(request);

        ArgumentCaptor<ExternalApiSettings> captor = ArgumentCaptor.forClass(ExternalApiSettings.class);
        verify(repository).save(captor.capture());
        assertNull(captor.getValue().getJudgeModelFilename());
    }

    @Test
    void toDto_includesJudgeFields() {
        entity.setJudgeEnabled(true);
        entity.setJudgeModelFilename("judge.gguf");
        entity.setJudgeScoreThreshold(6.0);
        when(repository.findBySingletonGuard("SINGLETON")).thenReturn(Optional.of(entity));

        ExternalApiSettingsDto dto = service.getSettings();

        assertTrue(dto.judgeEnabled());
        assertEquals("judge.gguf", dto.judgeModelFilename());
        assertEquals(6.0, dto.judgeScoreThreshold());
    }
}
