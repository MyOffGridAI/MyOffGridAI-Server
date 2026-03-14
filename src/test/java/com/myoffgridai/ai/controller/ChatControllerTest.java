package com.myoffgridai.ai.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.myoffgridai.ai.dto.SendMessageRequest;
import com.myoffgridai.ai.model.Conversation;
import com.myoffgridai.ai.model.Message;
import com.myoffgridai.ai.model.MessageRole;
import com.myoffgridai.ai.repository.MessageRepository;
import com.myoffgridai.ai.service.ChatService;
import com.myoffgridai.auth.model.Role;
import com.myoffgridai.auth.model.User;
import com.myoffgridai.auth.service.AuthService;
import com.myoffgridai.auth.service.JwtService;
import com.myoffgridai.config.CaptivePortalRedirectFilter;
import com.myoffgridai.config.TestSecurityConfig;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.MediaType;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.test.web.servlet.MockMvc;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.authentication;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(ChatController.class)
@Import(TestSecurityConfig.class)
class ChatControllerTest {

    @Autowired private MockMvc mockMvc;
    @Autowired private ObjectMapper objectMapper;

    @MockBean private ChatService chatService;
    @MockBean private MessageRepository messageRepository;
    @MockBean private JwtService jwtService;
    @MockBean private AuthService authService;
    @MockBean private UserDetailsService userDetailsService;
    @MockitoBean private CaptivePortalRedirectFilter captivePortalRedirectFilter;

    private User testUser;
    private UUID userId;
    private UUID conversationId;
    private Conversation testConversation;

    @BeforeEach
    void setUp() {
        userId = UUID.randomUUID();
        conversationId = UUID.randomUUID();

        testUser = new User();
        testUser.setId(userId);
        testUser.setUsername("testuser");
        testUser.setDisplayName("Test User");
        testUser.setRole(Role.ROLE_MEMBER);
        testUser.setPasswordHash("$2a$10$dummy");
        testUser.setIsActive(true);

        testConversation = new Conversation();
        testConversation.setId(conversationId);
        testConversation.setUser(testUser);
        testConversation.setTitle("Test Conversation");
        testConversation.setMessageCount(0);
        testConversation.setCreatedAt(Instant.now());
        testConversation.setUpdatedAt(Instant.now());
    }

    @Test
    void createConversation_authenticated_returns201() throws Exception {
        when(chatService.createConversation(any(UUID.class), any())).thenReturn(testConversation);

        mockMvc.perform(post("/api/chat/conversations")
                        .with(authentication(createAuth(testUser)))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"title\":\"Test\"}"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.title").value("Test Conversation"));
    }

    @Test
    void createConversation_unauthenticated_returns401() throws Exception {
        mockMvc.perform(post("/api/chat/conversations")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"title\":\"Test\"}"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void listConversations_returns200() throws Exception {
        Page<Conversation> page = new PageImpl<>(List.of(testConversation));
        when(chatService.getConversations(any(UUID.class), anyBoolean(), any()))
                .thenReturn(page);

        mockMvc.perform(get("/api/chat/conversations")
                        .with(authentication(createAuth(testUser))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true));
    }

    @Test
    void getConversation_returns200() throws Exception {
        when(chatService.getConversation(any(UUID.class), any(UUID.class)))
                .thenReturn(testConversation);

        mockMvc.perform(get("/api/chat/conversations/" + conversationId)
                        .with(authentication(createAuth(testUser))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.title").value("Test Conversation"));
    }

    @Test
    void deleteConversation_returns200() throws Exception {
        mockMvc.perform(delete("/api/chat/conversations/" + conversationId)
                        .with(authentication(createAuth(testUser))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("Conversation deleted"));

        verify(chatService).deleteConversation(conversationId, userId);
    }

    @Test
    void archiveConversation_returns200() throws Exception {
        mockMvc.perform(put("/api/chat/conversations/" + conversationId + "/archive")
                        .with(authentication(createAuth(testUser))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("Conversation archived"));

        verify(chatService).archiveConversation(conversationId, userId);
    }

    @Test
    void sendMessage_sync_returns200() throws Exception {
        Message assistantMsg = new Message();
        assistantMsg.setId(UUID.randomUUID());
        assistantMsg.setRole(MessageRole.ASSISTANT);
        assistantMsg.setContent("Hello!");
        assistantMsg.setTokenCount(5);
        assistantMsg.setHasRagContext(false);
        assistantMsg.setCreatedAt(Instant.now());

        when(chatService.sendMessage(any(UUID.class), any(UUID.class), anyString()))
                .thenReturn(assistantMsg);

        SendMessageRequest request = new SendMessageRequest("Hello", false);

        mockMvc.perform(post("/api/chat/conversations/" + conversationId + "/messages")
                        .with(authentication(createAuth(testUser)))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.content").value("Hello!"));
    }

    @Test
    void sendMessage_unauthenticated_returns401() throws Exception {
        SendMessageRequest request = new SendMessageRequest("Hello", false);

        mockMvc.perform(post("/api/chat/conversations/" + conversationId + "/messages")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void listMessages_returns200() throws Exception {
        when(chatService.getConversation(any(UUID.class), any(UUID.class)))
                .thenReturn(testConversation);
        when(messageRepository.findByConversationIdOrderByCreatedAtAsc(
                any(UUID.class), any(PageRequest.class)))
                .thenReturn(new PageImpl<>(List.of()));

        mockMvc.perform(get("/api/chat/conversations/" + conversationId + "/messages")
                        .with(authentication(createAuth(testUser))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true));
    }

    private org.springframework.security.authentication.UsernamePasswordAuthenticationToken
    createAuth(User user) {
        return new org.springframework.security.authentication.UsernamePasswordAuthenticationToken(
                user, null, user.getAuthorities());
    }
}
