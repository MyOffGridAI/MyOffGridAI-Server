package com.myoffgridai.proactive.service;

import com.myoffgridai.ai.service.OllamaService;
import com.myoffgridai.auth.model.Role;
import com.myoffgridai.auth.model.User;
import com.myoffgridai.auth.repository.UserRepository;
import com.myoffgridai.system.model.SystemConfig;
import com.myoffgridai.system.service.SystemConfigService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class SystemHealthMonitorTest {

    @Mock private OllamaService ollamaService;
    @Mock private UserRepository userRepository;
    @Mock private NotificationService notificationService;
    @Mock private SystemConfigService systemConfigService;

    private SystemHealthMonitor monitor;

    @BeforeEach
    void setUp() {
        SystemConfig config = new SystemConfig();
        config.setKnowledgeStoragePath("/tmp");
        lenient().when(systemConfigService.getConfig()).thenReturn(config);

        monitor = new SystemHealthMonitor(ollamaService, userRepository, notificationService, systemConfigService);
    }

    @Test
    void checkOllamaAvailability_unavailable_notifiesAdmins() {
        when(ollamaService.isAvailable()).thenReturn(false);
        User admin = createAdmin();
        when(userRepository.findAllByRole(Role.ROLE_OWNER)).thenReturn(List.of(admin));
        when(userRepository.findAllByRole(Role.ROLE_ADMIN)).thenReturn(List.of());

        monitor.checkOllamaAvailability();

        verify(notificationService).createNotification(
                eq(admin.getId()), eq("Ollama Unavailable"), anyString(), any(), any(), any());
    }

    @Test
    void checkOllamaAvailability_available_noNotification() {
        when(ollamaService.isAvailable()).thenReturn(true);

        monitor.checkOllamaAvailability();

        verifyNoInteractions(notificationService);
    }

    @Test
    void checkHeapUsage_belowThreshold_noNotification() {
        // Default JVM heap usage is typically well below 90%
        monitor.checkHeapUsage();

        verifyNoInteractions(notificationService);
    }

    @Test
    void checkSystemHealth_runsAllChecks() {
        when(ollamaService.isAvailable()).thenReturn(true);

        monitor.checkSystemHealth();

        verify(ollamaService).isAvailable();
    }

    @Test
    void checkOllamaAvailability_cooldown_suppressesRepeat() {
        when(ollamaService.isAvailable()).thenReturn(false);
        User admin = createAdmin();
        when(userRepository.findAllByRole(Role.ROLE_OWNER)).thenReturn(List.of(admin));
        when(userRepository.findAllByRole(Role.ROLE_ADMIN)).thenReturn(List.of());

        monitor.checkOllamaAvailability();
        monitor.checkOllamaAvailability();

        // Only one notification due to cooldown
        verify(notificationService, times(1)).createNotification(
                eq(admin.getId()), eq("Ollama Unavailable"), anyString(), any(), any(), any());
    }

    @Test
    void checkDiskSpace_sufficientDisk_noNotification() {
        // Default disk space should be above 500MB on any dev machine
        monitor.checkDiskSpace();

        verifyNoInteractions(notificationService);
    }

    private User createAdmin() {
        User user = new User();
        user.setId(UUID.randomUUID());
        user.setUsername("admin");
        user.setDisplayName("Admin");
        user.setRole(Role.ROLE_OWNER);
        user.setPasswordHash("hash");
        return user;
    }
}
