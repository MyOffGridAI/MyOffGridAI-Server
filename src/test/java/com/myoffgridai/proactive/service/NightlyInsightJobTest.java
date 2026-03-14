package com.myoffgridai.proactive.service;

import com.myoffgridai.auth.model.Role;
import com.myoffgridai.auth.model.User;
import com.myoffgridai.auth.repository.UserRepository;
import com.myoffgridai.proactive.model.Insight;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class NightlyInsightJobTest {

    @Mock private UserRepository userRepository;
    @Mock private InsightGeneratorService insightGeneratorService;

    private NightlyInsightJob job;

    @BeforeEach
    void setUp() {
        job = new NightlyInsightJob(userRepository, insightGeneratorService);
    }

    @Test
    void generateNightlyInsights_processesAllActiveUsers() {
        User user1 = createUser("user1");
        User user2 = createUser("user2");
        when(userRepository.findByIsActiveTrue()).thenReturn(List.of(user1, user2));
        when(insightGeneratorService.generateInsightForUser(any(UUID.class)))
                .thenReturn(List.of(new Insight()));

        job.generateNightlyInsights();

        verify(insightGeneratorService).generateInsightForUser(user1.getId());
        verify(insightGeneratorService).generateInsightForUser(user2.getId());
    }

    @Test
    void generateNightlyInsights_noActiveUsers_doesNothing() {
        when(userRepository.findByIsActiveTrue()).thenReturn(List.of());

        job.generateNightlyInsights();

        verifyNoInteractions(insightGeneratorService);
    }

    @Test
    void generateNightlyInsights_oneUserFails_continuesWithOthers() {
        User user1 = createUser("user1");
        User user2 = createUser("user2");
        when(userRepository.findByIsActiveTrue()).thenReturn(List.of(user1, user2));
        when(insightGeneratorService.generateInsightForUser(user1.getId()))
                .thenThrow(new RuntimeException("Ollama down"));
        when(insightGeneratorService.generateInsightForUser(user2.getId()))
                .thenReturn(List.of(new Insight()));

        job.generateNightlyInsights();

        verify(insightGeneratorService).generateInsightForUser(user1.getId());
        verify(insightGeneratorService).generateInsightForUser(user2.getId());
    }

    private User createUser(String username) {
        User user = new User();
        user.setId(UUID.randomUUID());
        user.setUsername(username);
        user.setDisplayName(username);
        user.setRole(Role.ROLE_MEMBER);
        user.setPasswordHash("hash");
        return user;
    }
}
