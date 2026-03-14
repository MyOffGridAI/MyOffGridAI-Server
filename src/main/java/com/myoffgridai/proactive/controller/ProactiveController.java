package com.myoffgridai.proactive.controller;

import com.myoffgridai.auth.model.User;
import com.myoffgridai.common.response.ApiResponse;
import com.myoffgridai.config.AppConstants;
import com.myoffgridai.proactive.dto.InsightDto;
import com.myoffgridai.proactive.dto.NotificationDto;
import com.myoffgridai.proactive.model.Insight;
import com.myoffgridai.proactive.model.InsightCategory;
import com.myoffgridai.proactive.model.Notification;
import com.myoffgridai.proactive.service.InsightGeneratorService;
import com.myoffgridai.proactive.service.InsightService;
import com.myoffgridai.proactive.service.NotificationService;
import com.myoffgridai.proactive.service.NotificationSseRegistry;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * REST controller for proactive insights and notifications.
 * Provides endpoints for insight management, notification management,
 * and real-time SSE notification streaming.
 */
@RestController
public class ProactiveController {

    private static final Logger log = LoggerFactory.getLogger(ProactiveController.class);

    private final InsightService insightService;
    private final InsightGeneratorService insightGeneratorService;
    private final NotificationService notificationService;
    private final NotificationSseRegistry notificationSseRegistry;

    /**
     * Constructs the proactive controller.
     *
     * @param insightService          the insight service
     * @param insightGeneratorService the insight generator service
     * @param notificationService     the notification service
     * @param notificationSseRegistry the notification SSE registry
     */
    public ProactiveController(InsightService insightService,
                               InsightGeneratorService insightGeneratorService,
                               NotificationService notificationService,
                               NotificationSseRegistry notificationSseRegistry) {
        this.insightService = insightService;
        this.insightGeneratorService = insightGeneratorService;
        this.notificationService = notificationService;
        this.notificationSseRegistry = notificationSseRegistry;
    }

    // ── Insights ─────────────────────────────────────────────────────────────

    /**
     * Gets active insights for the authenticated user with optional category filter.
     *
     * @param principal the authenticated user
     * @param category  optional category filter
     * @param page      the page number (0-based)
     * @param size      the page size
     * @return paginated list of insight DTOs
     */
    @GetMapping(AppConstants.INSIGHTS_API_PATH)
    public ResponseEntity<ApiResponse<List<InsightDto>>> getInsights(
            @AuthenticationPrincipal User principal,
            @RequestParam(required = false) InsightCategory category,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        size = Math.min(size, AppConstants.MAX_PAGE_SIZE);
        Page<Insight> insights;
        if (category != null) {
            insights = insightService.getInsightsByCategory(
                    principal.getId(), category, PageRequest.of(page, size));
        } else {
            insights = insightService.getInsights(principal.getId(), PageRequest.of(page, size));
        }
        List<InsightDto> dtos = insights.getContent().stream()
                .map(InsightDto::from).toList();
        return ResponseEntity.ok(ApiResponse.paginated(
                dtos, insights.getTotalElements(), page, size));
    }

    /**
     * Triggers on-demand insight generation for the authenticated user.
     *
     * @param principal the authenticated user
     * @return list of generated insight DTOs
     */
    @PostMapping(AppConstants.INSIGHTS_API_PATH + "/generate")
    public ResponseEntity<ApiResponse<List<InsightDto>>> generateInsights(
            @AuthenticationPrincipal User principal) {
        log.info("User {} requested on-demand insight generation", principal.getId());
        List<Insight> insights = insightGeneratorService.generateInsightForUser(principal.getId());
        List<InsightDto> dtos = insights.stream().map(InsightDto::from).toList();
        return ResponseEntity.ok(ApiResponse.success(dtos, "Generated " + dtos.size() + " insights"));
    }

    /**
     * Marks an insight as read.
     *
     * @param principal the authenticated user
     * @param insightId the insight ID
     * @return the updated insight DTO
     */
    @PutMapping(AppConstants.INSIGHTS_API_PATH + "/{insightId}/read")
    public ResponseEntity<ApiResponse<InsightDto>> markInsightRead(
            @AuthenticationPrincipal User principal,
            @PathVariable UUID insightId) {
        Insight insight = insightService.markRead(insightId, principal.getId());
        return ResponseEntity.ok(ApiResponse.success(InsightDto.from(insight), "Insight marked as read"));
    }

    /**
     * Dismisses an insight so it no longer appears in active queries.
     *
     * @param principal the authenticated user
     * @param insightId the insight ID
     * @return the updated insight DTO
     */
    @PutMapping(AppConstants.INSIGHTS_API_PATH + "/{insightId}/dismiss")
    public ResponseEntity<ApiResponse<InsightDto>> dismissInsight(
            @AuthenticationPrincipal User principal,
            @PathVariable UUID insightId) {
        Insight insight = insightService.dismiss(insightId, principal.getId());
        return ResponseEntity.ok(ApiResponse.success(InsightDto.from(insight), "Insight dismissed"));
    }

    /**
     * Gets the count of unread insights for the authenticated user.
     *
     * @param principal the authenticated user
     * @return the unread count wrapped in a map
     */
    @GetMapping(AppConstants.INSIGHTS_API_PATH + "/unread-count")
    public ResponseEntity<ApiResponse<Map<String, Long>>> getInsightUnreadCount(
            @AuthenticationPrincipal User principal) {
        long count = insightService.getUnreadCount(principal.getId());
        return ResponseEntity.ok(ApiResponse.success(Map.of("unreadCount", count)));
    }

    // ── Notifications ────────────────────────────────────────────────────────

    /**
     * Gets notifications for the authenticated user with pagination.
     *
     * @param principal  the authenticated user
     * @param unreadOnly if true, only return unread notifications
     * @param page       the page number (0-based)
     * @param size       the page size
     * @return list of notification DTOs
     */
    @GetMapping(AppConstants.NOTIFICATIONS_API_PATH)
    public ResponseEntity<ApiResponse<List<NotificationDto>>> getNotifications(
            @AuthenticationPrincipal User principal,
            @RequestParam(defaultValue = "false") boolean unreadOnly,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        size = Math.min(size, AppConstants.MAX_PAGE_SIZE);
        if (unreadOnly) {
            List<Notification> notifications = notificationService.getUnreadNotifications(principal.getId());
            List<NotificationDto> dtos = notifications.stream()
                    .map(NotificationDto::from).toList();
            return ResponseEntity.ok(ApiResponse.success(dtos));
        }
        Page<Notification> notifications = notificationService.getNotifications(
                principal.getId(), PageRequest.of(page, size));
        List<NotificationDto> dtos = notifications.getContent().stream()
                .map(NotificationDto::from).toList();
        return ResponseEntity.ok(ApiResponse.paginated(
                dtos, notifications.getTotalElements(), page, size));
    }

    /**
     * Marks a notification as read.
     *
     * @param principal      the authenticated user
     * @param notificationId the notification ID
     * @return the updated notification DTO
     */
    @PutMapping(AppConstants.NOTIFICATIONS_API_PATH + "/{notificationId}/read")
    public ResponseEntity<ApiResponse<NotificationDto>> markNotificationRead(
            @AuthenticationPrincipal User principal,
            @PathVariable UUID notificationId) {
        Notification notification = notificationService.markRead(notificationId, principal.getId());
        return ResponseEntity.ok(ApiResponse.success(
                NotificationDto.from(notification), "Notification marked as read"));
    }

    /**
     * Marks all notifications as read for the authenticated user.
     *
     * @param principal the authenticated user
     * @return success response
     */
    @PutMapping(AppConstants.NOTIFICATIONS_API_PATH + "/read-all")
    public ResponseEntity<ApiResponse<Void>> markAllNotificationsRead(
            @AuthenticationPrincipal User principal) {
        notificationService.markAllRead(principal.getId());
        return ResponseEntity.ok(ApiResponse.success(null, "All notifications marked as read"));
    }

    /**
     * Gets the count of unread notifications for the authenticated user.
     *
     * @param principal the authenticated user
     * @return the unread count wrapped in a map
     */
    @GetMapping(AppConstants.NOTIFICATIONS_API_PATH + "/unread-count")
    public ResponseEntity<ApiResponse<Map<String, Long>>> getNotificationUnreadCount(
            @AuthenticationPrincipal User principal) {
        long count = notificationService.getUnreadCount(principal.getId());
        return ResponseEntity.ok(ApiResponse.success(Map.of("unreadCount", count)));
    }

    /**
     * Deletes a notification.
     *
     * @param principal      the authenticated user
     * @param notificationId the notification ID
     * @return success response
     */
    @DeleteMapping(AppConstants.NOTIFICATIONS_API_PATH + "/{notificationId}")
    public ResponseEntity<ApiResponse<Void>> deleteNotification(
            @AuthenticationPrincipal User principal,
            @PathVariable UUID notificationId) {
        notificationService.deleteNotification(notificationId, principal.getId());
        return ResponseEntity.ok(ApiResponse.success(null, "Notification deleted"));
    }

    // ── SSE Notification Stream ──────────────────────────────────────────────

    /**
     * Opens a Server-Sent Events stream for real-time notifications.
     *
     * @param principal the authenticated user
     * @return an SSE emitter that broadcasts notifications in real-time
     */
    @GetMapping(value = AppConstants.NOTIFICATIONS_API_PATH + "/stream",
            produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public SseEmitter streamNotifications(
            @AuthenticationPrincipal User principal) {
        SseEmitter emitter = new SseEmitter(AppConstants.SSE_EMITTER_TIMEOUT_MS);
        notificationSseRegistry.register(principal.getId(), emitter);
        log.info("User {} opened notification SSE stream", principal.getId());
        return emitter;
    }
}
