package com.myoffgridai.proactive.service;

import com.myoffgridai.common.exception.EntityNotFoundException;
import com.myoffgridai.proactive.model.Insight;
import com.myoffgridai.proactive.model.InsightCategory;
import com.myoffgridai.proactive.repository.InsightRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class InsightServiceTest {

    @Mock private InsightRepository insightRepository;

    private InsightService service;
    private UUID userId;

    @BeforeEach
    void setUp() {
        service = new InsightService(insightRepository);
        userId = UUID.randomUUID();
    }

    @Test
    void getInsights_returnsPaginatedInsights() {
        Insight insight = createInsight();
        when(insightRepository.findByUserIdAndIsDismissedFalseOrderByGeneratedAtDesc(eq(userId), any()))
                .thenReturn(new PageImpl<>(List.of(insight)));

        var page = service.getInsights(userId, PageRequest.of(0, 20));

        assertEquals(1, page.getTotalElements());
    }

    @Test
    void getInsightsByCategory_filtersByCategory() {
        Insight insight = createInsight();
        insight.setCategory(InsightCategory.HOMESTEAD);
        when(insightRepository.findByUserIdAndCategoryAndIsDismissedFalse(
                eq(userId), eq(InsightCategory.HOMESTEAD), any()))
                .thenReturn(new PageImpl<>(List.of(insight)));

        var page = service.getInsightsByCategory(userId, InsightCategory.HOMESTEAD, PageRequest.of(0, 20));

        assertEquals(1, page.getTotalElements());
    }

    @Test
    void getUnreadInsights_returnsUnreadOnly() {
        Insight insight = createInsight();
        when(insightRepository.findByUserIdAndIsReadFalseAndIsDismissedFalse(userId))
                .thenReturn(List.of(insight));

        List<Insight> result = service.getUnreadInsights(userId);

        assertEquals(1, result.size());
    }

    @Test
    void markRead_setsReadAndReadAt() {
        Insight insight = createInsight();
        when(insightRepository.findByIdAndUserId(insight.getId(), userId))
                .thenReturn(Optional.of(insight));
        when(insightRepository.save(any(Insight.class))).thenAnswer(i -> i.getArgument(0));

        Insight result = service.markRead(insight.getId(), userId);

        assertTrue(result.getIsRead());
        assertNotNull(result.getReadAt());
    }

    @Test
    void markRead_notFound_throwsException() {
        UUID insightId = UUID.randomUUID();
        when(insightRepository.findByIdAndUserId(insightId, userId))
                .thenReturn(Optional.empty());

        assertThrows(EntityNotFoundException.class,
                () -> service.markRead(insightId, userId));
    }

    @Test
    void dismiss_setsIsDismissed() {
        Insight insight = createInsight();
        when(insightRepository.findByIdAndUserId(insight.getId(), userId))
                .thenReturn(Optional.of(insight));
        when(insightRepository.save(any(Insight.class))).thenAnswer(i -> i.getArgument(0));

        Insight result = service.dismiss(insight.getId(), userId);

        assertTrue(result.getIsDismissed());
    }

    @Test
    void dismiss_notFound_throwsException() {
        UUID insightId = UUID.randomUUID();
        when(insightRepository.findByIdAndUserId(insightId, userId))
                .thenReturn(Optional.empty());

        assertThrows(EntityNotFoundException.class,
                () -> service.dismiss(insightId, userId));
    }

    @Test
    void getUnreadCount_returnsCount() {
        when(insightRepository.countByUserIdAndIsReadFalseAndIsDismissedFalse(userId)).thenReturn(3L);

        assertEquals(3L, service.getUnreadCount(userId));
    }

    @Test
    void deleteAllForUser_deletesAll() {
        service.deleteAllForUser(userId);

        verify(insightRepository).deleteByUserId(userId);
    }

    private Insight createInsight() {
        Insight insight = new Insight();
        insight.setId(UUID.randomUUID());
        insight.setUserId(userId);
        insight.setContent("Test insight content");
        insight.setCategory(InsightCategory.GENERAL);
        insight.setGeneratedAt(Instant.now());
        return insight;
    }
}
