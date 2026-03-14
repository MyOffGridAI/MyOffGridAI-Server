package com.myoffgridai.privacy.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.myoffgridai.auth.model.Role;
import com.myoffgridai.auth.model.User;
import com.myoffgridai.auth.service.AuthService;
import com.myoffgridai.auth.service.JwtService;
import com.myoffgridai.config.CaptivePortalRedirectFilter;
import com.myoffgridai.config.JwtAuthFilter;
import com.myoffgridai.config.TestSecurityConfig;
import com.myoffgridai.privacy.dto.*;
import com.myoffgridai.privacy.model.AuditLog;
import com.myoffgridai.privacy.model.AuditOutcome;
import com.myoffgridai.privacy.service.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.data.domain.PageImpl;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(PrivacyController.class)
@Import(TestSecurityConfig.class)
class PrivacyControllerTest {

    @Autowired private MockMvc mockMvc;
    @Autowired private ObjectMapper objectMapper;

    @MockitoBean private FortressService fortressService;
    @MockitoBean private AuditService auditService;
    @MockitoBean private SovereigntyReportService sovereigntyReportService;
    @MockitoBean private DataExportService dataExportService;
    @MockitoBean private DataWipeService dataWipeService;
    @MockitoBean private JwtService jwtService;
    @MockitoBean private JwtAuthFilter jwtAuthFilter;
    @MockitoBean private AuthService authService;
    @MockitoBean private CaptivePortalRedirectFilter captivePortalRedirectFilter;

    private User testOwner;
    private User regularUser;
    private UUID ownerId;
    private UUID memberId;

    @BeforeEach
    void setUp() {
        ownerId = UUID.randomUUID();
        testOwner = new User();
        testOwner.setId(ownerId);
        testOwner.setUsername("owner");
        testOwner.setDisplayName("Owner");
        testOwner.setRole(Role.ROLE_OWNER);
        testOwner.setPasswordHash("hash");

        memberId = UUID.randomUUID();
        regularUser = new User();
        regularUser.setId(memberId);
        regularUser.setUsername("member");
        regularUser.setDisplayName("Member");
        regularUser.setRole(Role.ROLE_MEMBER);
        regularUser.setPasswordHash("hash");
    }

    // ── Fortress ────────────────────────────────────────────────────────────

    @Test
    void getFortressStatus_returnsStatus() throws Exception {
        FortressStatus status = new FortressStatus(
                true, Instant.now(), "owner", true);
        when(fortressService.getFortressStatus()).thenReturn(status);

        mockMvc.perform(get("/api/privacy/fortress/status").with(user(testOwner)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.enabled").value(true))
                .andExpect(jsonPath("$.data.enabledByUsername").value("owner"))
                .andExpect(jsonPath("$.data.verified").value(true));
    }

    @Test
    void enableFortress_ownerRole_succeeds() throws Exception {
        doNothing().when(fortressService).enable(ownerId);

        mockMvc.perform(post("/api/privacy/fortress/enable").with(user(testOwner)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.status").value("enabled"))
                .andExpect(jsonPath("$.message").value(
                        "Fortress mode enabled — all outbound traffic blocked"));

        verify(fortressService).enable(ownerId);
    }

    @Test
    void enableFortress_memberRole_denied() throws Exception {
        mockMvc.perform(post("/api/privacy/fortress/enable").with(user(regularUser)))
                .andExpect(status().isForbidden());
    }

    @Test
    void disableFortress_ownerRole_succeeds() throws Exception {
        doNothing().when(fortressService).disable(ownerId);

        mockMvc.perform(post("/api/privacy/fortress/disable").with(user(testOwner)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.status").value("disabled"))
                .andExpect(jsonPath("$.message").value(
                        "Fortress mode disabled — outbound traffic restored"));

        verify(fortressService).disable(ownerId);
    }

    // ── Sovereignty Report ──────────────────────────────────────────────────

    @Test
    void getSovereigntyReport_returnsReport() throws Exception {
        Instant now = Instant.now();
        FortressStatus fortressStatus = new FortressStatus(true, now, "owner", true);
        DataInventory dataInventory = new DataInventory(5, 100, 20, 3, 2, 10);
        AuditSummary auditSummary = new AuditSummary(
                50, 2, 1, now.minusSeconds(86400), now);
        SovereigntyReport report = new SovereigntyReport(
                now,
                fortressStatus,
                "BLOCKED — All outbound internet traffic is blocked by iptables",
                dataInventory,
                auditSummary,
                "AES-256 encryption available for data exports",
                "DISABLED — No telemetry collected",
                now
        );
        when(sovereigntyReportService.generateReport(ownerId)).thenReturn(report);

        mockMvc.perform(get("/api/privacy/sovereignty-report").with(user(testOwner)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.fortressStatus.enabled").value(true))
                .andExpect(jsonPath("$.data.dataInventory.conversationCount").value(5))
                .andExpect(jsonPath("$.data.dataInventory.messageCount").value(100))
                .andExpect(jsonPath("$.data.auditSummary.successCount").value(50))
                .andExpect(jsonPath("$.data.encryptionStatus").value(
                        "AES-256 encryption available for data exports"));
    }

    // ── Audit Logs ──────────────────────────────────────────────────────────

    @Test
    void getAuditLogs_ownerSeeAll() throws Exception {
        AuditLog log1 = createAuditLog(ownerId, "owner");
        AuditLog log2 = createAuditLog(memberId, "member");
        when(auditService.getAuditLogs(any()))
                .thenReturn(new PageImpl<>(List.of(log1, log2)));

        mockMvc.perform(get("/api/privacy/audit-logs").with(user(testOwner)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data[0].username").value("owner"))
                .andExpect(jsonPath("$.data[1].username").value("member"))
                .andExpect(jsonPath("$.totalElements").value(2));
    }

    @Test
    void getAuditLogs_memberSeesOwnOnly() throws Exception {
        AuditLog log1 = createAuditLog(memberId, "member");
        when(auditService.getAuditLogsForUser(eq(memberId), any()))
                .thenReturn(new PageImpl<>(List.of(log1)));

        mockMvc.perform(get("/api/privacy/audit-logs").with(user(regularUser)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data[0].username").value("member"))
                .andExpect(jsonPath("$.totalElements").value(1));
    }

    // ── Data Export ─────────────────────────────────────────────────────────

    @Test
    void exportData_returnsEncryptedFile() throws Exception {
        byte[] encryptedData = "encrypted-zip-content".getBytes();
        when(dataExportService.exportUserData(eq(ownerId), eq("test12345678")))
                .thenReturn(encryptedData);

        mockMvc.perform(post("/api/privacy/export")
                        .with(user(testOwner))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"passphrase\":\"test12345678\"}"))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_OCTET_STREAM))
                .andExpect(header().string("Content-Disposition",
                        "attachment; filename=\"myoffgridai-export-" + ownerId + ".enc\""))
                .andExpect(content().bytes(encryptedData));
    }

    // ── Data Wipe ───────────────────────────────────────────────────────────

    @Test
    void wipeData_ownerRole_succeeds() throws Exception {
        UUID targetId = UUID.randomUUID();
        WipeResult result = new WipeResult(targetId, 10, Instant.now(), true);
        when(dataWipeService.wipeUser(targetId)).thenReturn(result);

        mockMvc.perform(delete("/api/privacy/wipe?targetId=" + targetId)
                        .with(user(testOwner)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.success").value(true))
                .andExpect(jsonPath("$.data.stepsCompleted").value(10))
                .andExpect(jsonPath("$.message").value("Data wipe completed successfully"));

        verify(dataWipeService).wipeUser(targetId);
    }

    @Test
    void wipeData_memberRole_denied() throws Exception {
        mockMvc.perform(delete("/api/privacy/wipe").with(user(regularUser)))
                .andExpect(status().isForbidden());
    }

    @Test
    void wipeSelfData_anyRole_succeeds() throws Exception {
        WipeResult result = new WipeResult(memberId, 10, Instant.now(), true);
        when(dataWipeService.wipeUser(memberId)).thenReturn(result);

        mockMvc.perform(delete("/api/privacy/wipe/self").with(user(regularUser)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.success").value(true))
                .andExpect(jsonPath("$.message").value("Your data has been wiped successfully"));

        verify(dataWipeService).wipeUser(memberId);
    }

    // ── Unauthenticated ─────────────────────────────────────────────────────

    @Test
    void endpoints_unauthenticated_returns401() throws Exception {
        mockMvc.perform(get("/api/privacy/fortress/status"))
                .andExpect(status().isUnauthorized());
    }

    // ── Helpers ──────────────────────────────────────────────────────────────

    private AuditLog createAuditLog(UUID userId, String username) {
        AuditLog log = new AuditLog();
        log.setId(UUID.randomUUID());
        log.setUserId(userId);
        log.setUsername(username);
        log.setAction("GET_STATUS");
        log.setResourceType("fortress");
        log.setResourceId("config");
        log.setHttpMethod("GET");
        log.setRequestPath("/api/privacy/fortress/status");
        log.setOutcome(AuditOutcome.SUCCESS);
        log.setResponseStatus(200);
        log.setDurationMs(15);
        log.setTimestamp(Instant.now());
        return log;
    }
}
