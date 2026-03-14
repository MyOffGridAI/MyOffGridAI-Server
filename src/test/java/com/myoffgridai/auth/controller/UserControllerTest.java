package com.myoffgridai.auth.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.myoffgridai.auth.dto.UpdateUserRequest;
import com.myoffgridai.auth.dto.UserDetailDto;
import com.myoffgridai.auth.dto.UserSummaryDto;
import com.myoffgridai.auth.model.Role;
import com.myoffgridai.auth.model.User;
import com.myoffgridai.auth.service.AuthService;
import com.myoffgridai.auth.service.JwtService;
import com.myoffgridai.auth.service.UserService;
import com.myoffgridai.common.exception.EntityNotFoundException;
import com.myoffgridai.common.exception.GlobalExceptionHandler;
import com.myoffgridai.config.CaptivePortalRedirectFilter;
import com.myoffgridai.config.JwtAuthFilter;
import com.myoffgridai.config.TestSecurityConfig;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.data.domain.PageImpl;
import org.springframework.http.MediaType;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.test.web.servlet.MockMvc;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(UserController.class)
@Import({TestSecurityConfig.class, GlobalExceptionHandler.class})
class UserControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private UserService userService;

    @MockBean
    private AuthService authService;

    @MockBean
    private JwtService jwtService;

    @MockBean
    private JwtAuthFilter jwtAuthFilter;

    @MockBean
    private UserDetailsService userDetailsService;

    @MockitoBean
    private CaptivePortalRedirectFilter captivePortalRedirectFilter;

    private User ownerPrincipal() {
        User owner = new User();
        owner.setId(UUID.randomUUID());
        owner.setUsername("owner");
        owner.setDisplayName("Owner");
        owner.setPasswordHash("hash");
        owner.setRole(Role.ROLE_OWNER);
        owner.setIsActive(true);
        return owner;
    }

    private User adminPrincipal() {
        User admin = new User();
        admin.setId(UUID.randomUUID());
        admin.setUsername("admin");
        admin.setDisplayName("Admin");
        admin.setPasswordHash("hash");
        admin.setRole(Role.ROLE_ADMIN);
        admin.setIsActive(true);
        return admin;
    }

    private User memberPrincipal() {
        User member = new User();
        member.setId(UUID.randomUUID());
        member.setUsername("member");
        member.setDisplayName("Member");
        member.setPasswordHash("hash");
        member.setRole(Role.ROLE_MEMBER);
        member.setIsActive(true);
        return member;
    }

    // ── GET /api/users ──────────────────────────────────────────────────────

    @Test
    void listUsers_asOwner_shouldReturn200() throws Exception {
        UserSummaryDto dto = new UserSummaryDto(UUID.randomUUID(), "user1", "User 1", Role.ROLE_MEMBER, true);
        when(userService.listUsers(0, 20)).thenReturn(new PageImpl<>(List.of(dto)));

        mockMvc.perform(get("/api/users").with(user(ownerPrincipal())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data[0].username").value("user1"));
    }

    @Test
    void listUsers_asAdmin_shouldReturn200() throws Exception {
        when(userService.listUsers(0, 20)).thenReturn(new PageImpl<>(List.of()));

        mockMvc.perform(get("/api/users").with(user(adminPrincipal())))
                .andExpect(status().isOk());
    }

    @Test
    void listUsers_asMember_shouldReturn403() throws Exception {
        mockMvc.perform(get("/api/users").with(user(memberPrincipal())))
                .andExpect(status().isForbidden());
    }

    @Test
    void listUsers_unauthenticated_shouldReturn401() throws Exception {
        mockMvc.perform(get("/api/users"))
                .andExpect(status().isUnauthorized());
    }

    // ── GET /api/users/{id} ─────────────────────────────────────────────────

    @Test
    void getUser_asOwner_shouldReturn200() throws Exception {
        UUID userId = UUID.randomUUID();
        UserDetailDto detail = new UserDetailDto(
                userId, "user1", "user@test.com", "User 1", Role.ROLE_MEMBER,
                true, Instant.now(), Instant.now(), null);
        when(userService.getUserById(userId)).thenReturn(detail);

        mockMvc.perform(get("/api/users/" + userId).with(user(ownerPrincipal())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.username").value("user1"));
    }

    @Test
    void getUser_memberViewingOwnProfile_shouldReturn200() throws Exception {
        User member = memberPrincipal();
        UUID ownId = member.getId();
        UserDetailDto detail = new UserDetailDto(
                ownId, "member", null, "Member", Role.ROLE_MEMBER,
                true, Instant.now(), Instant.now(), null);
        when(userService.getUserById(ownId)).thenReturn(detail);

        mockMvc.perform(get("/api/users/" + ownId).with(user(member)))
                .andExpect(status().isOk());
    }

    @Test
    void getUser_memberViewingOtherProfile_shouldReturn403() throws Exception {
        UUID otherId = UUID.randomUUID();

        mockMvc.perform(get("/api/users/" + otherId).with(user(memberPrincipal())))
                .andExpect(status().isForbidden());
    }

    @Test
    void getUser_notFound_shouldReturn404() throws Exception {
        UUID userId = UUID.randomUUID();
        when(userService.getUserById(userId))
                .thenThrow(new EntityNotFoundException("User not found: " + userId));

        mockMvc.perform(get("/api/users/" + userId).with(user(ownerPrincipal())))
                .andExpect(status().isNotFound());
    }

    // ── PUT /api/users/{id} ─────────────────────────────────────────────────

    @Test
    void updateUser_asOwner_shouldReturn200() throws Exception {
        UUID userId = UUID.randomUUID();
        UserDetailDto updated = new UserDetailDto(
                userId, "user1", "new@test.com", "Updated Name", Role.ROLE_ADMIN,
                true, Instant.now(), Instant.now(), null);
        when(userService.updateUser(eq(userId), anyString(), anyString(), any()))
                .thenReturn(updated);

        mockMvc.perform(put("/api/users/" + userId)
                        .with(user(ownerPrincipal()))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(
                                new UpdateUserRequest("Updated Name", "new@test.com", Role.ROLE_ADMIN))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.displayName").value("Updated Name"));
    }

    @Test
    void updateUser_asMember_shouldReturn403() throws Exception {
        UUID userId = UUID.randomUUID();
        mockMvc.perform(put("/api/users/" + userId)
                        .with(user(memberPrincipal()))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(
                                new UpdateUserRequest("Name", null, null))))
                .andExpect(status().isForbidden());
    }

    // ── PUT /api/users/{id}/deactivate ──────────────────────────────────────

    @Test
    void deactivateUser_asOwner_shouldReturn200() throws Exception {
        UUID userId = UUID.randomUUID();
        doNothing().when(userService).deactivateUser(userId);

        mockMvc.perform(put("/api/users/" + userId + "/deactivate")
                        .with(user(ownerPrincipal())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("User deactivated"));
    }

    @Test
    void deactivateUser_asAdmin_shouldReturn403() throws Exception {
        UUID userId = UUID.randomUUID();
        mockMvc.perform(put("/api/users/" + userId + "/deactivate")
                        .with(user(adminPrincipal())))
                .andExpect(status().isForbidden());
    }

    // ── DELETE /api/users/{id} ──────────────────────────────────────────────

    @Test
    void deleteUser_asOwner_shouldReturn200() throws Exception {
        UUID userId = UUID.randomUUID();
        doNothing().when(userService).deleteUser(userId);

        mockMvc.perform(delete("/api/users/" + userId)
                        .with(user(ownerPrincipal())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("User deleted"));
    }

    @Test
    void deleteUser_asAdmin_shouldReturn403() throws Exception {
        UUID userId = UUID.randomUUID();
        mockMvc.perform(delete("/api/users/" + userId)
                        .with(user(adminPrincipal())))
                .andExpect(status().isForbidden());
    }

    @Test
    void deleteUser_unauthenticated_shouldReturn401() throws Exception {
        UUID userId = UUID.randomUUID();
        mockMvc.perform(delete("/api/users/" + userId))
                .andExpect(status().isUnauthorized());
    }
}
