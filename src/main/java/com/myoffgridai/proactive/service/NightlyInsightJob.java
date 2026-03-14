package com.myoffgridai.proactive.service;

import com.myoffgridai.auth.model.User;
import com.myoffgridai.auth.repository.UserRepository;
import com.myoffgridai.proactive.model.Insight;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.time.Instant;
import java.util.List;

/**
 * Nightly scheduled job that generates proactive insights for all active users.
 * Runs at 3am daily.
 */
@Component
public class NightlyInsightJob {

    private static final Logger log = LoggerFactory.getLogger(NightlyInsightJob.class);

    private final UserRepository userRepository;
    private final InsightGeneratorService insightGeneratorService;

    /**
     * Constructs the nightly insight job.
     *
     * @param userRepository          the user repository
     * @param insightGeneratorService the insight generator service
     */
    public NightlyInsightJob(UserRepository userRepository,
                             InsightGeneratorService insightGeneratorService) {
        this.userRepository = userRepository;
        this.insightGeneratorService = insightGeneratorService;
    }

    /**
     * Runs at 3am daily to generate insights for all active users.
     * Each user is processed independently — one failure does not stop others.
     */
    @Scheduled(cron = "0 0 3 * * *")
    public void generateNightlyInsights() {
        Instant start = Instant.now();
        List<User> activeUsers = userRepository.findByIsActiveTrue();
        log.info("Nightly insight job started: {} active users", activeUsers.size());

        int totalInsights = 0;
        int failedUsers = 0;

        for (User user : activeUsers) {
            try {
                List<Insight> insights = insightGeneratorService.generateInsightForUser(user.getId());
                totalInsights += insights.size();
            } catch (Exception e) {
                failedUsers++;
                log.error("Failed to generate insights for user {}: {}", user.getId(), e.getMessage());
            }
        }

        Duration duration = Duration.between(start, Instant.now());
        log.info("Nightly insight job completed: {} insights generated for {} users ({} failed) in {}ms",
                totalInsights, activeUsers.size(), failedUsers, duration.toMillis());
    }
}
