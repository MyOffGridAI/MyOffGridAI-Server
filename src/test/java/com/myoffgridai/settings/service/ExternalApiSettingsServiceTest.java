package com.myoffgridai.settings.service;

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
        when(repository.findBySingletonGuard("SINGLETON")).thenReturn(Optional.of(entity));

        ExternalApiSettingsDto dto = service.getSettings();

        // DTO has boolean flags, not keys
        assertTrue(dto.anthropicKeyConfigured());
        assertTrue(dto.braveKeyConfigured());
    }

    @Test
    void updateSettings_updatesKeyWhenProvided() {
        when(repository.findBySingletonGuard("SINGLETON")).thenReturn(Optional.of(entity));
        when(repository.save(any())).thenReturn(entity);

        UpdateExternalApiSettingsRequest request = new UpdateExternalApiSettingsRequest(
                "new-anthropic-key", "claude-sonnet-4-20250514", true,
                "new-brave-key", true, 1024, 10
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

        UpdateExternalApiSettingsRequest request = new UpdateExternalApiSettingsRequest(
                null, "claude-sonnet-4-20250514", true,
                null, true, 512, 5
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

        UpdateExternalApiSettingsRequest request = new UpdateExternalApiSettingsRequest(
                "", "claude-sonnet-4-20250514", false,
                null, false, 512, 5
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
}
