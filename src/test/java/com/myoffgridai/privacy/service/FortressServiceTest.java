package com.myoffgridai.privacy.service;

import com.myoffgridai.auth.model.Role;
import com.myoffgridai.auth.model.User;
import com.myoffgridai.auth.repository.UserRepository;
import com.myoffgridai.privacy.dto.FortressStatus;
import com.myoffgridai.system.model.SystemConfig;
import com.myoffgridai.system.service.SystemConfigService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * Unit tests for {@link FortressService}.
 */
@ExtendWith(MockitoExtension.class)
class FortressServiceTest {

    @Mock
    private SystemConfigService systemConfigService;

    @Mock
    private UserRepository userRepository;

    private FortressService fortressService;

    @BeforeEach
    void setUp() {
        // Construct with mockMode=true so no ProcessBuilder/iptables commands are executed
        fortressService = new FortressService(systemConfigService, userRepository, true);
    }

    @Test
    void enable_mockMode_updatesConfigOnly() {
        UUID userId = UUID.randomUUID();
        SystemConfig config = new SystemConfig();
        config.setFortressEnabled(true);
        when(systemConfigService.setFortressEnabled(true, userId)).thenReturn(config);

        fortressService.enable(userId);

        verify(systemConfigService).setFortressEnabled(true, userId);
        // In mock mode, no ProcessBuilder should be invoked — we verify by checking
        // that only systemConfigService was interacted with (no exceptions from iptables)
        verifyNoInteractions(userRepository);
    }

    @Test
    void disable_mockMode_updatesConfigOnly() {
        UUID userId = UUID.randomUUID();
        SystemConfig config = new SystemConfig();
        config.setFortressEnabled(false);
        when(systemConfigService.setFortressEnabled(false, userId)).thenReturn(config);

        fortressService.disable(userId);

        verify(systemConfigService).setFortressEnabled(false, userId);
        verifyNoInteractions(userRepository);
    }

    @Test
    void getFortressStatus_enabledWithUser() {
        UUID userId = UUID.randomUUID();
        Instant enabledAt = Instant.parse("2026-03-14T10:00:00Z");

        SystemConfig config = new SystemConfig();
        config.setFortressEnabled(true);
        config.setFortressEnabledAt(enabledAt);
        config.setFortressEnabledByUserId(userId);
        when(systemConfigService.getConfig()).thenReturn(config);

        User user = new User();
        user.setId(userId);
        user.setUsername("admin");
        user.setPasswordHash("hashed");
        user.setRole(Role.ROLE_OWNER);
        user.setDisplayName("Admin User");
        when(userRepository.findById(userId)).thenReturn(Optional.of(user));

        FortressStatus status = fortressService.getFortressStatus();

        assertTrue(status.enabled());
        assertEquals(enabledAt, status.enabledAt());
        assertEquals("admin", status.enabledByUsername());
        assertTrue(status.verified()); // mock mode always returns true for verify
    }

    @Test
    void getFortressStatus_disabled() {
        SystemConfig config = new SystemConfig();
        config.setFortressEnabled(false);
        config.setFortressEnabledAt(null);
        config.setFortressEnabledByUserId(null);
        when(systemConfigService.getConfig()).thenReturn(config);

        FortressStatus status = fortressService.getFortressStatus();

        assertFalse(status.enabled());
        assertNull(status.enabledAt());
        assertNull(status.enabledByUsername());
        assertTrue(status.verified()); // mock mode always returns true
        verifyNoInteractions(userRepository);
    }

    @Test
    void isFortressActive_returnsTrue() {
        SystemConfig config = new SystemConfig();
        config.setFortressEnabled(true);
        when(systemConfigService.getConfig()).thenReturn(config);

        assertTrue(fortressService.isFortressActive());
    }

    @Test
    void isFortressActive_returnsFalse() {
        SystemConfig config = new SystemConfig();
        config.setFortressEnabled(false);
        when(systemConfigService.getConfig()).thenReturn(config);

        assertFalse(fortressService.isFortressActive());
    }
}
