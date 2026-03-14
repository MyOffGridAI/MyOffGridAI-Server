package com.myoffgridai.integration;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.myoffgridai.ai.repository.ConversationRepository;
import com.myoffgridai.ai.repository.MessageRepository;
import com.myoffgridai.auth.dto.*;
import com.myoffgridai.auth.model.Role;
import com.myoffgridai.auth.repository.UserRepository;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
class UserIntegrationTest extends BaseIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private MessageRepository messageRepository;

    @Autowired
    private ConversationRepository conversationRepository;

    private String ownerToken;
    private String memberToken;
    private String memberId;

    @BeforeEach
    void setup() throws Exception {
        messageRepository.deleteAll();
        conversationRepository.deleteAll();
        userRepository.deleteAll();

        // Register owner
        MvcResult ownerResult = mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(
                                new RegisterRequest("owner", null, "Owner", "pass", Role.ROLE_OWNER))))
                .andReturn();
        var ownerTree = objectMapper.readTree(ownerResult.getResponse().getContentAsString());
        ownerToken = ownerTree.path("data").path("accessToken").asText();

        // Register member
        MvcResult memberResult = mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(
                                new RegisterRequest("member", "member@test.com", "Member", "pass", Role.ROLE_MEMBER))))
                .andReturn();
        var memberTree = objectMapper.readTree(memberResult.getResponse().getContentAsString());
        memberToken = memberTree.path("data").path("accessToken").asText();
        memberId = memberTree.path("data").path("user").path("id").asText();
    }

    @Test
    @Order(1)
    void listUsers_asOwner_shouldReturnAll() throws Exception {
        mockMvc.perform(get("/api/users")
                        .header("Authorization", "Bearer " + ownerToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.length()").value(2));
    }

    @Test
    @Order(2)
    void listUsers_asMember_shouldReturn403() throws Exception {
        mockMvc.perform(get("/api/users")
                        .header("Authorization", "Bearer " + memberToken))
                .andExpect(status().isForbidden());
    }

    @Test
    @Order(3)
    void getUser_memberViewingOwnProfile_shouldReturn200() throws Exception {
        mockMvc.perform(get("/api/users/" + memberId)
                        .header("Authorization", "Bearer " + memberToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.username").value("member"));
    }

    @Test
    @Order(4)
    void updateUser_asOwner_shouldSucceed() throws Exception {
        UpdateUserRequest updateRequest = new UpdateUserRequest("Updated Name", "new@test.com", Role.ROLE_ADMIN);

        mockMvc.perform(put("/api/users/" + memberId)
                        .header("Authorization", "Bearer " + ownerToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(updateRequest)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.displayName").value("Updated Name"));
    }

    @Test
    @Order(5)
    void deactivateUser_asOwner_shouldSucceed() throws Exception {
        mockMvc.perform(put("/api/users/" + memberId + "/deactivate")
                        .header("Authorization", "Bearer " + ownerToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("User deactivated"));

        // Verify user is deactivated
        var user = userRepository.findById(java.util.UUID.fromString(memberId));
        assertThat(user).isPresent();
        assertThat(user.get().getIsActive()).isFalse();
    }

    @Test
    @Order(6)
    void deleteUser_asOwner_shouldSucceed() throws Exception {
        mockMvc.perform(delete("/api/users/" + memberId)
                        .header("Authorization", "Bearer " + ownerToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("User deleted"));

        assertThat(userRepository.findById(java.util.UUID.fromString(memberId))).isEmpty();
    }

    @Test
    @Order(7)
    void deleteUser_asMember_shouldReturn403() throws Exception {
        mockMvc.perform(delete("/api/users/" + memberId)
                        .header("Authorization", "Bearer " + memberToken))
                .andExpect(status().isForbidden());
    }
}
