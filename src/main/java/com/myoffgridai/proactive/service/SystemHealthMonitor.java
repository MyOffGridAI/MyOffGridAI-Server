package com.myoffgridai.proactive.service;

import com.myoffgridai.ai.service.OllamaService;
import com.myoffgridai.auth.model.Role;
import com.myoffgridai.auth.model.User;
import com.myoffgridai.auth.repository.UserRepository;
import com.myoffgridai.config.AppConstants;
import com.myoffgridai.proactive.model.NotificationType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.io.File;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Monitors system health (disk, Ollama, heap) and notifies OWNER/ADMIN users
 * when thresholds are breached. Rate-limits alerts to avoid notification spam.
 */
@Component
public class SystemHealthMonitor {

    private static final Logger log = LoggerFactory.getLogger(SystemHealthMonitor.class);

    private static final String ALERT_DISK = "disk-space";
    private static final String ALERT_OLLAMA = "ollama-unavailable";
    private static final String ALERT_HEAP = "high-heap-usage";

    private final OllamaService ollamaService;
    private final UserRepository userRepository;
    private final NotificationService notificationService;

    private final Map<String, Instant> lastAlertTime = new ConcurrentHashMap<>();

    /**
     * Constructs the system health monitor.
     *
     * @param ollamaService       the Ollama service for availability checks
     * @param userRepository      the user repository
     * @param notificationService the notification service
     */
    public SystemHealthMonitor(OllamaService ollamaService,
                               UserRepository userRepository,
                               NotificationService notificationService) {
        this.ollamaService = ollamaService;
        this.userRepository = userRepository;
        this.notificationService = notificationService;
    }

    /**
     * Runs periodic health checks for disk space, Ollama availability, and heap usage.
     */
    @Scheduled(fixedDelayString = "${app.health.check.interval-ms:300000}")
    public void checkSystemHealth() {
        checkDiskSpace();
        checkOllamaAvailability();
        checkHeapUsage();
    }

    void checkDiskSpace() {
        File storageDir = new File(AppConstants.KNOWLEDGE_STORAGE_BASE_PATH).getParentFile();
        if (storageDir == null) {
            storageDir = new File(AppConstants.KNOWLEDGE_STORAGE_BASE_PATH);
        }

        long freeSpaceMb = storageDir.getUsableSpace() / (1024 * 1024);
        if (freeSpaceMb < AppConstants.DISK_ALERT_THRESHOLD_MB) {
            if (shouldAlert(ALERT_DISK)) {
                String msg = String.format("Disk space is critically low: %dMB remaining (threshold: %dMB)",
                        freeSpaceMb, AppConstants.DISK_ALERT_THRESHOLD_MB);
                notifyAdmins("Low Disk Space", msg);
                log.warn(msg);
            }
        }
    }

    void checkOllamaAvailability() {
        if (!ollamaService.isAvailable()) {
            if (shouldAlert(ALERT_OLLAMA)) {
                notifyAdmins("Ollama Unavailable",
                        "The Ollama LLM service is not responding. AI features are currently unavailable.");
                log.warn("Ollama service is unavailable");
            }
        }
    }

    void checkHeapUsage() {
        Runtime runtime = Runtime.getRuntime();
        long usedMemory = runtime.totalMemory() - runtime.freeMemory();
        long maxMemory = runtime.maxMemory();
        double usagePercent = (double) usedMemory / maxMemory * 100;

        if (usagePercent > 90.0) {
            if (shouldAlert(ALERT_HEAP)) {
                String msg = String.format("JVM heap usage is %.1f%% (%dMB / %dMB). Consider restarting the server.",
                        usagePercent, usedMemory / (1024 * 1024), maxMemory / (1024 * 1024));
                notifyAdmins("High Memory Usage", msg);
                log.warn(msg);
            }
        }
    }

    private boolean shouldAlert(String alertType) {
        Instant lastAlert = lastAlertTime.get(alertType);
        Instant cooldownEnd = Instant.now().minus(AppConstants.HEALTH_ALERT_COOLDOWN_MINUTES, ChronoUnit.MINUTES);
        if (lastAlert != null && lastAlert.isAfter(cooldownEnd)) {
            return false;
        }
        lastAlertTime.put(alertType, Instant.now());
        return true;
    }

    private void notifyAdmins(String title, String body) {
        List<User> admins = new ArrayList<>();
        admins.addAll(userRepository.findAllByRole(Role.ROLE_OWNER));
        admins.addAll(userRepository.findAllByRole(Role.ROLE_ADMIN));

        for (User admin : admins) {
            notificationService.createNotification(
                    admin.getId(), title, body, NotificationType.SYSTEM_HEALTH, null);
        }
    }
}
