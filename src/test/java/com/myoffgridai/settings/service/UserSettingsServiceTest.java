package com.myoffgridai.settings.service;

import com.myoffgridai.auth.model.User;
import com.myoffgridai.auth.repository.UserRepository;
import com.myoffgridai.settings.dto.UpdateUserSettingsRequest;
import com.myoffgridai.settings.dto.UserSettingsDto;
import com.myoffgridai.settings.model.UserSettings;
import com.myoffgridai.settings.repository.UserSettingsRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/**
 * Tests for {@link UserSettingsService}.
 */
@ExtendWith(MockitoExtension.class)
class UserSettingsServiceTest {

    @Mock
    private UserSettingsRepository userSettingsRepository;

    @Mock
    private UserRepository userRepository;

    @InjectMocks
    private UserSettingsService service;

    private UUID userId;
    private User user;
    private UserSettings entity;

    @BeforeEach
    void setUp() {
        userId = UUID.randomUUID();
        user = new User();
        user.setId(userId);
        user.setUsername("testuser");
        user.setDisplayName("Test User");
        user.setPasswordHash("hash");

        entity = new UserSettings();
        entity.setId(UUID.randomUUID());
        entity.setUser(user);
        entity.setThemePreference("system");
    }

    @Test
    void getSettings_createsDefaultsWhenNotFound() {
        when(userSettingsRepository.findByUserId(userId)).thenReturn(Optional.empty());
        when(userRepository.getReferenceById(userId)).thenReturn(user);
        when(userSettingsRepository.save(any())).thenReturn(entity);

        UserSettingsDto dto = service.getSettings(userId);

        assertEquals("system", dto.themePreference());
        verify(userSettingsRepository).save(any());
    }

    @Test
    void getSettings_returnsExistingSettings() {
        entity.setThemePreference("dark");
        when(userSettingsRepository.findByUserId(userId)).thenReturn(Optional.of(entity));

        UserSettingsDto dto = service.getSettings(userId);

        assertEquals("dark", dto.themePreference());
        verify(userSettingsRepository, never()).save(any());
    }

    @Test
    void getSettings_defaultThemeIsSystem() {
        when(userSettingsRepository.findByUserId(userId)).thenReturn(Optional.empty());
        when(userRepository.getReferenceById(userId)).thenReturn(user);

        UserSettings defaultSettings = new UserSettings();
        defaultSettings.setUser(user);
        when(userSettingsRepository.save(any())).thenReturn(defaultSettings);

        UserSettingsDto dto = service.getSettings(userId);

        assertEquals("system", dto.themePreference());
    }

    @Test
    void updateSettings_updatesThemePreference() {
        when(userSettingsRepository.findByUserId(userId)).thenReturn(Optional.of(entity));
        when(userSettingsRepository.save(any())).thenReturn(entity);

        UpdateUserSettingsRequest request = new UpdateUserSettingsRequest("dark");
        service.updateSettings(userId, request);

        ArgumentCaptor<UserSettings> captor = ArgumentCaptor.forClass(UserSettings.class);
        verify(userSettingsRepository).save(captor.capture());

        assertEquals("dark", captor.getValue().getThemePreference());
    }

    @Test
    void updateSettings_createsDefaultsThenUpdates() {
        when(userSettingsRepository.findByUserId(userId)).thenReturn(Optional.empty());
        when(userRepository.getReferenceById(userId)).thenReturn(user);
        when(userSettingsRepository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));

        UpdateUserSettingsRequest request = new UpdateUserSettingsRequest("light");
        UserSettingsDto dto = service.updateSettings(userId, request);

        assertEquals("light", dto.themePreference());
        // save called twice: once for create, once for update
        verify(userSettingsRepository, times(2)).save(any());
    }

    @Test
    void updateSettings_returnsUpdatedDto() {
        entity.setThemePreference("system");
        when(userSettingsRepository.findByUserId(userId)).thenReturn(Optional.of(entity));
        when(userSettingsRepository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));

        UpdateUserSettingsRequest request = new UpdateUserSettingsRequest("light");
        UserSettingsDto dto = service.updateSettings(userId, request);

        assertEquals("light", dto.themePreference());
    }

    @Test
    void updateSettings_switchesFromDarkToSystem() {
        entity.setThemePreference("dark");
        when(userSettingsRepository.findByUserId(userId)).thenReturn(Optional.of(entity));
        when(userSettingsRepository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));

        UpdateUserSettingsRequest request = new UpdateUserSettingsRequest("system");
        UserSettingsDto dto = service.updateSettings(userId, request);

        assertEquals("system", dto.themePreference());
    }
}
