package com.myoffgridai.events.controller;

import com.myoffgridai.auth.model.User;
import com.myoffgridai.common.response.ApiResponse;
import com.myoffgridai.config.AppConstants;
import com.myoffgridai.events.dto.CreateEventRequest;
import com.myoffgridai.events.dto.ScheduledEventDto;
import com.myoffgridai.events.dto.UpdateEventRequest;
import com.myoffgridai.events.model.ScheduledEvent;
import com.myoffgridai.events.service.ScheduledEventService;
import jakarta.validation.Valid;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * REST controller for scheduled event management.
 *
 * <p>All endpoints are user-scoped via the authenticated principal.</p>
 */
@RestController
@RequestMapping(AppConstants.EVENTS_API_PATH)
public class ScheduledEventController {

    private static final Logger log = LoggerFactory.getLogger(ScheduledEventController.class);

    private final ScheduledEventService eventService;

    public ScheduledEventController(ScheduledEventService eventService) {
        this.eventService = eventService;
    }

    /**
     * Lists all events for the authenticated user with pagination.
     */
    @GetMapping
    public ResponseEntity<ApiResponse<List<ScheduledEventDto>>> listEvents(
            @AuthenticationPrincipal User principal,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        Page<ScheduledEvent> events = eventService.listEvents(principal.getId(), page, size);
        List<ScheduledEventDto> dtos = events.getContent().stream()
                .map(ScheduledEventDto::from).toList();
        return ResponseEntity.ok(ApiResponse.paginated(
                dtos, events.getTotalElements(), page, size));
    }

    /**
     * Gets a single event by ID.
     */
    @GetMapping("/{eventId}")
    public ResponseEntity<ApiResponse<ScheduledEventDto>> getEvent(
            @AuthenticationPrincipal User principal,
            @PathVariable java.util.UUID eventId) {
        ScheduledEvent event = eventService.getEvent(eventId, principal.getId());
        return ResponseEntity.ok(ApiResponse.success(ScheduledEventDto.from(event)));
    }

    /**
     * Creates a new scheduled event.
     */
    @PostMapping
    public ResponseEntity<ApiResponse<ScheduledEventDto>> createEvent(
            @AuthenticationPrincipal User principal,
            @Valid @RequestBody CreateEventRequest request) {
        ScheduledEvent event = eventService.createEvent(principal.getId(), request);
        log.info("User {} created event '{}'", principal.getId(), event.getName());
        return ResponseEntity.ok(ApiResponse.success(
                ScheduledEventDto.from(event), "Event created"));
    }

    /**
     * Updates an existing event.
     */
    @PutMapping("/{eventId}")
    public ResponseEntity<ApiResponse<ScheduledEventDto>> updateEvent(
            @AuthenticationPrincipal User principal,
            @PathVariable java.util.UUID eventId,
            @Valid @RequestBody UpdateEventRequest request) {
        ScheduledEvent event = eventService.updateEvent(eventId, principal.getId(), request);
        return ResponseEntity.ok(ApiResponse.success(
                ScheduledEventDto.from(event), "Event updated"));
    }

    /**
     * Deletes an event.
     */
    @DeleteMapping("/{eventId}")
    public ResponseEntity<ApiResponse<Void>> deleteEvent(
            @AuthenticationPrincipal User principal,
            @PathVariable java.util.UUID eventId) {
        eventService.deleteEvent(eventId, principal.getId());
        return ResponseEntity.ok(ApiResponse.success(null, "Event deleted"));
    }

    /**
     * Toggles the enabled/disabled state of an event.
     */
    @PutMapping("/{eventId}/toggle")
    public ResponseEntity<ApiResponse<ScheduledEventDto>> toggleEvent(
            @AuthenticationPrincipal User principal,
            @PathVariable java.util.UUID eventId) {
        ScheduledEvent event = eventService.toggleEvent(eventId, principal.getId());
        String message = event.getIsEnabled() ? "Event enabled" : "Event disabled";
        return ResponseEntity.ok(ApiResponse.success(ScheduledEventDto.from(event), message));
    }
}
