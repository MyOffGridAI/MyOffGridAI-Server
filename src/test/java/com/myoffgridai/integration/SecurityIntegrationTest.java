package com.myoffgridai.integration;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.myoffgridai.auth.dto.LoginRequest;
import com.myoffgridai.auth.dto.RegisterRequest;
import com.myoffgridai.auth.model.Role;
import com.myoffgridai.auth.repository.UserRepository;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
class SecurityIntegrationTest extends BaseIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private UserRepository userRepository;

    @BeforeEach
    void clean() {
        userRepository.deleteAll();
    }

    // ── Public endpoints accessible without token ───────────────────────────

    @Test
    @Order(1)
    void publicEndpoint_login_shouldBeAccessible() throws Exception {
        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(
                                new LoginRequest("nobody", "pass"))))
                .andExpect(status().isUnauthorized()); // 401 from bad creds, not 403 from security
    }

    @Test
    @Order(2)
    void publicEndpoint_register_shouldBeAccessible() throws Exception {
        mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(
                                new RegisterRequest("newuser", null, "User", "pass", Role.ROLE_MEMBER))))
                .andExpect(status().isCreated());
    }

    @Test
    @Order(3)
    void publicEndpoint_actuatorHealth_shouldBeAccessible() throws Exception {
        mockMvc.perform(get("/actuator/health"))
                .andExpect(status().isOk());
    }

    // ── Protected endpoints return 401 without token ────────────────────────

    @Test
    @Order(4)
    void protectedEndpoint_users_withoutToken_shouldReturn401() throws Exception {
        mockMvc.perform(get("/api/users"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @Order(5)
    void protectedEndpoint_logout_withoutToken_shouldReturn401() throws Exception {
        mockMvc.perform(post("/api/auth/logout"))
                .andExpect(status().isUnauthorized());
    }

    // ── Role-based access enforcement ───────────────────────────────────────

    @Test
    @Order(6)
    void memberRole_cannotListUsers_shouldReturn403() throws Exception {
        // Register member
        MvcResult regResult = mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(
                                new RegisterRequest("member", null, "Member", "pass", Role.ROLE_MEMBER))))
                .andReturn();

        var tree = objectMapper.readTree(regResult.getResponse().getContentAsString());
        String token = tree.path("data").path("accessToken").asText();

        mockMvc.perform(get("/api/users")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isForbidden());
    }

    @Test
    @Order(7)
    void viewerRole_cannotDeleteUser_shouldReturn403() throws Exception {
        MvcResult regResult = mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(
                                new RegisterRequest("viewer", null, "Viewer", "pass", Role.ROLE_VIEWER))))
                .andReturn();

        var tree = objectMapper.readTree(regResult.getResponse().getContentAsString());
        String token = tree.path("data").path("accessToken").asText();
        String userId = tree.path("data").path("user").path("id").asText();

        mockMvc.perform(delete("/api/users/" + userId)
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isForbidden());
    }

    @Test
    @Order(8)
    void ownerRole_canAccessProtectedEndpoints() throws Exception {
        MvcResult regResult = mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(
                                new RegisterRequest("owner", null, "Owner", "pass", Role.ROLE_OWNER))))
                .andReturn();

        var tree = objectMapper.readTree(regResult.getResponse().getContentAsString());
        String token = tree.path("data").path("accessToken").asText();

        mockMvc.perform(get("/api/users")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk());
    }
}
