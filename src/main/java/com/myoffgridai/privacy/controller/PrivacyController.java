package com.myoffgridai.privacy.controller;

import com.myoffgridai.auth.model.User;
import com.myoffgridai.common.response.ApiResponse;
import com.myoffgridai.config.AppConstants;
import com.myoffgridai.privacy.dto.*;
import com.myoffgridai.privacy.model.AuditLog;
import com.myoffgridai.privacy.model.AuditOutcome;
import com.myoffgridai.privacy.service.*;
import jakarta.validation.Valid;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * REST controller for privacy, fortress, audit, export, and wipe operations.
 *
 * <p>All endpoints require authentication. Fortress and wipe operations
 * are restricted to OWNER or ADMIN roles.</p>
 */
@RestController
@RequestMapping(AppConstants.API_PRIVACY)
public class PrivacyController {

    private static final Logger log = LoggerFactory.getLogger(PrivacyController.class);

    private final FortressService fortressService;
    private final AuditService auditService;
    private final SovereigntyReportService sovereigntyReportService;
    private final DataExportService dataExportService;
    private final DataWipeService dataWipeService;

    /**
     * Constructs the privacy controller.
     *
     * @param fortressService         the fortress service
     * @param auditService            the audit service
     * @param sovereigntyReportService the sovereignty report service
     * @param dataExportService       the data export service
     * @param dataWipeService         the data wipe service
     */
    public PrivacyController(FortressService fortressService,
                             AuditService auditService,
                             SovereigntyReportService sovereigntyReportService,
                             DataExportService dataExportService,
                             DataWipeService dataWipeService) {
        this.fortressService = fortressService;
        this.auditService = auditService;
        this.sovereigntyReportService = sovereigntyReportService;
        this.dataExportService = dataExportService;
        this.dataWipeService = dataWipeService;
    }

    // ── Fortress ────────────────────────────────────────────────────────────

    /**
     * Gets the current fortress mode status.
     *
     * @return the fortress status
     */
    @GetMapping("/fortress/status")
    public ResponseEntity<ApiResponse<FortressStatus>> getFortressStatus() {
        FortressStatus status = fortressService.getFortressStatus();
        return ResponseEntity.ok(ApiResponse.success(status));
    }

    /**
     * Enables fortress mode (blocks all outbound internet traffic).
     * Restricted to OWNER or ADMIN roles.
     *
     * @param user the authenticated user
     * @return confirmation message
     */
    @PostMapping("/fortress/enable")
    @PreAuthorize("hasAnyAuthority('" + AppConstants.ROLE_OWNER + "', '" + AppConstants.ROLE_ADMIN + "')")
    public ResponseEntity<ApiResponse<Map<String, String>>> enableFortress(
            @AuthenticationPrincipal User user) {
        log.info("Fortress enable requested by user {}", user.getUsername());
        fortressService.enable(user.getId());
        return ResponseEntity.ok(ApiResponse.success(
                Map.of("status", "enabled"),
                "Fortress mode enabled — all outbound traffic blocked"));
    }

    /**
     * Disables fortress mode (restores normal network connectivity).
     * Restricted to OWNER or ADMIN roles.
     *
     * @param user the authenticated user
     * @return confirmation message
     */
    @PostMapping("/fortress/disable")
    @PreAuthorize("hasAnyAuthority('" + AppConstants.ROLE_OWNER + "', '" + AppConstants.ROLE_ADMIN + "')")
    public ResponseEntity<ApiResponse<Map<String, String>>> disableFortress(
            @AuthenticationPrincipal User user) {
        log.info("Fortress disable requested by user {}", user.getUsername());
        fortressService.disable(user.getId());
        return ResponseEntity.ok(ApiResponse.success(
                Map.of("status", "disabled"),
                "Fortress mode disabled — outbound traffic restored"));
    }

    // ── Sovereignty Report ──────────────────────────────────────────────────

    /**
     * Generates a sovereignty report for the authenticated user.
     *
     * @param user the authenticated user
     * @return the sovereignty report
     */
    @GetMapping("/sovereignty-report")
    public ResponseEntity<ApiResponse<SovereigntyReport>> getSovereigntyReport(
            @AuthenticationPrincipal User user) {
        SovereigntyReport report = sovereigntyReportService.generateReport(user.getId());
        return ResponseEntity.ok(ApiResponse.success(report));
    }

    // ── Audit Logs ──────────────────────────────────────────────────────────

    /**
     * Gets audit logs with optional filters. OWNER/ADMIN can view all logs;
     * regular users see only their own.
     *
     * @param user    the authenticated user
     * @param outcome optional outcome filter
     * @param page    the page number (0-based)
     * @param size    the page size
     * @return paginated audit logs
     */
    @GetMapping("/audit-logs")
    public ResponseEntity<ApiResponse<List<AuditLogDto>>> getAuditLogs(
            @AuthenticationPrincipal User user,
            @RequestParam(required = false) AuditOutcome outcome,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {

        size = Math.min(size, AppConstants.MAX_PAGE_SIZE);
        Page<AuditLog> logs;

        boolean isPrivileged = user.getAuthorities().stream()
                .anyMatch(a -> a.getAuthority().equals(AppConstants.ROLE_OWNER)
                        || a.getAuthority().equals(AppConstants.ROLE_ADMIN));

        if (isPrivileged) {
            if (outcome != null) {
                logs = auditService.getAuditLogsByOutcome(outcome, PageRequest.of(page, size));
            } else {
                logs = auditService.getAuditLogs(PageRequest.of(page, size));
            }
        } else {
            logs = auditService.getAuditLogsForUser(user.getId(), PageRequest.of(page, size));
        }

        List<AuditLogDto> dtos = logs.getContent().stream()
                .map(AuditLogDto::from)
                .toList();

        return ResponseEntity.ok(ApiResponse.paginated(
                dtos, logs.getTotalElements(), page, size));
    }

    // ── Data Export ─────────────────────────────────────────────────────────

    /**
     * Exports all user data as an AES-256-GCM encrypted ZIP archive.
     *
     * @param user    the authenticated user
     * @param request the export request with passphrase
     * @return the encrypted ZIP file
     */
    @PostMapping("/export")
    public ResponseEntity<byte[]> exportData(
            @AuthenticationPrincipal User user,
            @Valid @RequestBody ExportRequest request) {
        log.info("Data export requested by user {}", user.getUsername());

        byte[] encrypted = dataExportService.exportUserData(user.getId(), request.passphrase());

        return ResponseEntity.ok()
                .contentType(MediaType.APPLICATION_OCTET_STREAM)
                .header(HttpHeaders.CONTENT_DISPOSITION,
                        "attachment; filename=\"myoffgridai-export-" + user.getId() + ".enc\"")
                .body(encrypted);
    }

    // ── Data Wipe ───────────────────────────────────────────────────────────

    /**
     * Wipes all data for a target user. OWNER/ADMIN can wipe any user;
     * regular users can only wipe their own data.
     *
     * @param user     the authenticated user
     * @param targetId the target user ID to wipe (defaults to self)
     * @return the wipe result
     */
    @DeleteMapping("/wipe")
    @PreAuthorize("hasAnyAuthority('" + AppConstants.ROLE_OWNER + "', '" + AppConstants.ROLE_ADMIN + "')")
    public ResponseEntity<ApiResponse<WipeResult>> wipeData(
            @AuthenticationPrincipal User user,
            @RequestParam(required = false) UUID targetId) {

        UUID targetUserId = targetId != null ? targetId : user.getId();
        log.warn("Data wipe requested by user {} for target user {}", user.getUsername(), targetUserId);

        WipeResult result = dataWipeService.wipeUser(targetUserId);
        return ResponseEntity.ok(ApiResponse.success(result, "Data wipe completed successfully"));
    }

    /**
     * Wipes the authenticated user's own data. Available to all authenticated users.
     *
     * @param user the authenticated user
     * @return the wipe result
     */
    @DeleteMapping("/wipe/self")
    public ResponseEntity<ApiResponse<WipeResult>> wipeSelfData(
            @AuthenticationPrincipal User user) {
        log.warn("Self data wipe requested by user {}", user.getUsername());
        WipeResult result = dataWipeService.wipeUser(user.getId());
        return ResponseEntity.ok(ApiResponse.success(result, "Your data has been wiped successfully"));
    }
}
