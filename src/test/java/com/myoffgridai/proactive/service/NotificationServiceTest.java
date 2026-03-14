package com.myoffgridai.proactive.service;

import com.myoffgridai.common.exception.EntityNotFoundException;
import com.myoffgridai.proactive.model.Notification;
import com.myoffgridai.proactive.model.NotificationType;
import com.myoffgridai.proactive.repository.NotificationRepository;
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
class NotificationServiceTest {

    @Mock private NotificationRepository notificationRepository;
    @Mock private NotificationSseRegistry sseRegistry;

    private NotificationService service;
    private UUID userId;

    @BeforeEach
    void setUp() {
        service = new NotificationService(notificationRepository, sseRegistry);
        userId = UUID.randomUUID();
    }

    @Test
    void createNotification_persistsAndBroadcasts() {
        when(notificationRepository.save(any(Notification.class))).thenAnswer(i -> {
            Notification n = i.getArgument(0);
            n.setId(UUID.randomUUID());
            n.setCreatedAt(Instant.now());
            return n;
        });

        Notification result = service.createNotification(
                userId, "Test", "Body", NotificationType.GENERAL, null);

        assertNotNull(result.getId());
        assertEquals("Test", result.getTitle());
        assertEquals("Body", result.getBody());
        assertEquals(NotificationType.GENERAL, result.getType());
        verify(sseRegistry).broadcast(eq(userId), any(Notification.class));
    }

    @Test
    void getUnreadNotifications_returnsList() {
        Notification n = createNotification();
        when(notificationRepository.findByUserIdAndIsReadFalseOrderByCreatedAtDesc(userId))
                .thenReturn(List.of(n));

        List<Notification> result = service.getUnreadNotifications(userId);

        assertEquals(1, result.size());
    }

    @Test
    void getNotifications_returnsPaginated() {
        Notification n = createNotification();
        when(notificationRepository.findByUserIdOrderByCreatedAtDesc(eq(userId), any()))
                .thenReturn(new PageImpl<>(List.of(n)));

        var page = service.getNotifications(userId, PageRequest.of(0, 20));

        assertEquals(1, page.getTotalElements());
    }

    @Test
    void markRead_setsReadAndBroadcastsCount() {
        Notification n = createNotification();
        when(notificationRepository.findByIdAndUserId(n.getId(), userId))
                .thenReturn(Optional.of(n));
        when(notificationRepository.save(any(Notification.class))).thenAnswer(i -> i.getArgument(0));
        when(notificationRepository.countByUserIdAndIsReadFalse(userId)).thenReturn(0L);

        Notification result = service.markRead(n.getId(), userId);

        assertTrue(result.getIsRead());
        assertNotNull(result.getReadAt());
        verify(sseRegistry).broadcastUnreadCount(userId, 0);
    }

    @Test
    void markRead_notFound_throwsException() {
        UUID notifId = UUID.randomUUID();
        when(notificationRepository.findByIdAndUserId(notifId, userId))
                .thenReturn(Optional.empty());

        assertThrows(EntityNotFoundException.class,
                () -> service.markRead(notifId, userId));
    }

    @Test
    void markAllRead_updatesAndBroadcasts() {
        service.markAllRead(userId);

        verify(notificationRepository).markAllReadForUser(eq(userId), any(Instant.class));
        verify(sseRegistry).broadcastUnreadCount(userId, 0);
    }

    @Test
    void getUnreadCount_returnsCount() {
        when(notificationRepository.countByUserIdAndIsReadFalse(userId)).thenReturn(5L);

        assertEquals(5L, service.getUnreadCount(userId));
    }

    @Test
    void deleteNotification_deletesSuccessfully() {
        Notification n = createNotification();
        when(notificationRepository.findByIdAndUserId(n.getId(), userId))
                .thenReturn(Optional.of(n));

        service.deleteNotification(n.getId(), userId);

        verify(notificationRepository).delete(n);
    }

    @Test
    void deleteNotification_notFound_throwsException() {
        UUID notifId = UUID.randomUUID();
        when(notificationRepository.findByIdAndUserId(notifId, userId))
                .thenReturn(Optional.empty());

        assertThrows(EntityNotFoundException.class,
                () -> service.deleteNotification(notifId, userId));
    }

    @Test
    void deleteAllForUser_deletesAll() {
        service.deleteAllForUser(userId);

        verify(notificationRepository).deleteByUserId(userId);
    }

    private Notification createNotification() {
        Notification n = new Notification();
        n.setId(UUID.randomUUID());
        n.setUserId(userId);
        n.setTitle("Test");
        n.setBody("Test body");
        n.setType(NotificationType.GENERAL);
        n.setCreatedAt(Instant.now());
        return n;
    }
}
