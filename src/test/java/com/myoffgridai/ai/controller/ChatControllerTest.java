package com.myoffgridai.ai.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.myoffgridai.ai.dto.EditMessageRequest;
import com.myoffgridai.ai.dto.SendMessageRequest;
import com.myoffgridai.ai.model.Conversation;
import com.myoffgridai.ai.model.Message;
import com.myoffgridai.ai.model.MessageRole;
import com.myoffgridai.ai.repository.MessageRepository;
import com.myoffgridai.ai.service.ChatExportService;
import com.myoffgridai.ai.service.ChatService;
import com.myoffgridai.auth.model.Role;
import com.myoffgridai.auth.model.User;
import com.myoffgridai.knowledge.dto.KnowledgeDocumentDto;
import com.myoffgridai.knowledge.model.DocumentStatus;
import com.myoffgridai.auth.service.AuthService;
import com.myoffgridai.auth.service.JwtService;
import com.myoffgridai.config.CaptivePortalRedirectFilter;
import com.myoffgridai.config.TestSecurityConfig;
import reactor.core.publisher.Flux;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
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

/**
 * WebMvcTest-level controller tests for {@link ChatController}.
 *
 * <p>Validates HTTP status codes, JSON response shapes, and service delegation
 * for all chat conversation and message endpoints, including the P11 edit,
 * delete, branch, and regenerate operations.</p>
 */
@WebMvcTest(ChatController.class)
@Import(TestSecurityConfig.class)
class ChatControllerTest {

    @Autowired private MockMvc mockMvc;
    @Autowired private ObjectMapper objectMapper;

    @MockitoBean private ChatService chatService;
    @MockitoBean private ChatExportService chatExportService;
    @MockitoBean private MessageRepository messageRepository;
    @MockitoBean private JwtService jwtService;
    @MockitoBean private AuthService authService;
    @MockitoBean private UserDetailsService userDetailsService;
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
    void renameConversation_returns200() throws Exception {
        testConversation.setTitle("New Title");
        when(chatService.renameConversation(any(UUID.class), any(UUID.class), anyString()))
                .thenReturn(testConversation);

        mockMvc.perform(put("/api/chat/conversations/" + conversationId + "/title")
                        .with(authentication(createAuth(testUser)))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"title\":\"New Title\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.title").value("New Title"));
    }

    @Test
    void renameConversation_unauthenticated_returns401() throws Exception {
        mockMvc.perform(put("/api/chat/conversations/" + conversationId + "/title")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"title\":\"New Title\"}"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void searchConversations_returns200() throws Exception {
        Page<Conversation> page = new PageImpl<>(List.of(testConversation));
        when(chatService.searchConversations(any(UUID.class), anyString(), any()))
                .thenReturn(page);

        mockMvc.perform(get("/api/chat/conversations/search")
                        .param("q", "Test")
                        .with(authentication(createAuth(testUser))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data[0].title").value("Test Conversation"));
    }

    @Test
    void searchConversations_unauthenticated_returns401() throws Exception {
        mockMvc.perform(get("/api/chat/conversations/search")
                        .param("q", "Test"))
                .andExpect(status().isUnauthorized());
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

    // ── P11: editMessage tests ──────────────────────────────────────────

    @Test
    void editMessage_returns200() throws Exception {
        UUID messageId = UUID.randomUUID();
        Message editedMessage = new Message();
        editedMessage.setId(messageId);
        editedMessage.setRole(MessageRole.USER);
        editedMessage.setContent("updated content");
        editedMessage.setTokenCount(3);
        editedMessage.setHasRagContext(false);
        editedMessage.setCreatedAt(Instant.now());

        when(chatService.editMessage(any(UUID.class), any(UUID.class), any(UUID.class), anyString()))
                .thenReturn(editedMessage);

        EditMessageRequest request = new EditMessageRequest("updated content");

        mockMvc.perform(put("/api/chat/conversations/" + conversationId + "/messages/" + messageId)
                        .with(authentication(createAuth(testUser)))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.content").value("updated content"))
                .andExpect(jsonPath("$.data.role").value("USER"));

        verify(chatService).editMessage(conversationId, messageId, userId, "updated content");
    }

    @Test
    void editMessage_unauthenticated_returns401() throws Exception {
        UUID messageId = UUID.randomUUID();
        EditMessageRequest request = new EditMessageRequest("updated content");

        mockMvc.perform(put("/api/chat/conversations/" + conversationId + "/messages/" + messageId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isUnauthorized());
    }

    // ── P11: deleteMessage tests ─────────────────────────────────────────

    @Test
    void deleteMessage_returns200() throws Exception {
        UUID messageId = UUID.randomUUID();

        mockMvc.perform(delete("/api/chat/conversations/" + conversationId + "/messages/" + messageId)
                        .with(authentication(createAuth(testUser))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.message").value("Message deleted"));

        verify(chatService).deleteMessage(conversationId, messageId, userId);
    }

    @Test
    void deleteMessage_unauthenticated_returns401() throws Exception {
        UUID messageId = UUID.randomUUID();

        mockMvc.perform(delete("/api/chat/conversations/" + conversationId + "/messages/" + messageId))
                .andExpect(status().isUnauthorized());
    }

    // ── P11: branchConversation tests ────────────────────────────────────

    @Test
    void branchConversation_returns201() throws Exception {
        UUID messageId = UUID.randomUUID();

        Conversation branchedConversation = new Conversation();
        branchedConversation.setId(UUID.randomUUID());
        branchedConversation.setUser(testUser);
        branchedConversation.setTitle("Branched Conversation");
        branchedConversation.setMessageCount(3);
        branchedConversation.setCreatedAt(Instant.now());
        branchedConversation.setUpdatedAt(Instant.now());

        when(chatService.branchConversation(any(UUID.class), any(UUID.class), any(UUID.class), any()))
                .thenReturn(branchedConversation);

        mockMvc.perform(post("/api/chat/conversations/" + conversationId + "/branch/" + messageId)
                        .with(authentication(createAuth(testUser)))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"title\":\"My Branch\"}"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.title").value("Branched Conversation"))
                .andExpect(jsonPath("$.data.messageCount").value(3));

        verify(chatService).branchConversation(conversationId, messageId, userId, "My Branch");
    }

    @Test
    void branchConversation_noBody_returns201() throws Exception {
        UUID messageId = UUID.randomUUID();

        Conversation branchedConversation = new Conversation();
        branchedConversation.setId(UUID.randomUUID());
        branchedConversation.setUser(testUser);
        branchedConversation.setTitle("Auto Branch");
        branchedConversation.setMessageCount(2);
        branchedConversation.setCreatedAt(Instant.now());
        branchedConversation.setUpdatedAt(Instant.now());

        when(chatService.branchConversation(any(UUID.class), any(UUID.class), any(UUID.class), any()))
                .thenReturn(branchedConversation);

        mockMvc.perform(post("/api/chat/conversations/" + conversationId + "/branch/" + messageId)
                        .with(authentication(createAuth(testUser))))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.data.title").value("Auto Branch"));
    }

    @Test
    void branchConversation_unauthenticated_returns401() throws Exception {
        UUID messageId = UUID.randomUUID();

        mockMvc.perform(post("/api/chat/conversations/" + conversationId + "/branch/" + messageId))
                .andExpect(status().isUnauthorized());
    }

    // ── P11: regenerateMessage tests ─────────────────────────────────────

    @Test
    void regenerateMessage_delegatesToService() throws Exception {
        UUID messageId = UUID.randomUUID();

        // The regenerate endpoint creates an SseEmitter and subscribes to the Flux.
        // MockMvc does not natively support async SSE emitter responses — it throws
        // HttpMessageNotWritableException when trying to serialize SseEmitter.
        // We verify service delegation by catching the inevitable 500 and confirming
        // the ChatService.regenerateMessage call was made with the correct arguments.
        when(chatService.regenerateMessage(any(UUID.class), any(UUID.class), any(UUID.class)))
                .thenReturn(Flux.empty());

        mockMvc.perform(post("/api/chat/conversations/" + conversationId
                        + "/messages/" + messageId + "/regenerate")
                        .with(authentication(createAuth(testUser))))
                .andReturn();

        verify(chatService).regenerateMessage(conversationId, messageId, userId);
    }

    @Test
    void regenerateMessage_unauthenticated_returns401() throws Exception {
        UUID messageId = UUID.randomUUID();

        mockMvc.perform(post("/api/chat/conversations/" + conversationId
                        + "/messages/" + messageId + "/regenerate"))
                .andExpect(status().isUnauthorized());
    }

    // ── Export PDF tests ──────────────────────────────────────────────

    @Test
    void exportPdf_authenticated_returnsPdfBytes() throws Exception {
        byte[] pdfBytes = "%PDF-test".getBytes();
        when(chatExportService.generateConversationPdf(conversationId, userId))
                .thenReturn(pdfBytes);

        mockMvc.perform(get("/api/chat/conversations/" + conversationId + "/export-pdf")
                        .with(authentication(createAuth(testUser))))
                .andExpect(status().isOk())
                .andExpect(content().contentType("application/pdf"))
                .andExpect(content().bytes(pdfBytes));

        verify(chatExportService).generateConversationPdf(conversationId, userId);
    }

    @Test
    void exportPdf_unauthenticated_returns401() throws Exception {
        mockMvc.perform(get("/api/chat/conversations/" + conversationId + "/export-pdf"))
                .andExpect(status().isUnauthorized());
    }

    // ── Save to Library tests ──────────────────────────────────────────

    @Test
    void saveToLibrary_authenticated_returns201() throws Exception {
        UUID docId = UUID.randomUUID();
        KnowledgeDocumentDto dto = new KnowledgeDocumentDto(
                docId, "Test_Conversation.pdf", "Test Conversation",
                "application/pdf", 5000, DocumentStatus.PENDING, null, 0,
                Instant.now(), null, false, false,
                false, true, null);

        when(chatExportService.saveConversationToLibrary(conversationId, userId))
                .thenReturn(dto);

        mockMvc.perform(post("/api/chat/conversations/" + conversationId + "/save-to-library")
                        .with(authentication(createAuth(testUser))))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.displayName").value("Test Conversation"))
                .andExpect(jsonPath("$.data.mimeType").value("application/pdf"))
                .andExpect(jsonPath("$.data.status").value("PENDING"));

        verify(chatExportService).saveConversationToLibrary(conversationId, userId);
    }

    @Test
    void saveToLibrary_unauthenticated_returns401() throws Exception {
        mockMvc.perform(post("/api/chat/conversations/" + conversationId + "/save-to-library"))
                .andExpect(status().isUnauthorized());
    }

    private org.springframework.security.authentication.UsernamePasswordAuthenticationToken
    createAuth(User user) {
        return new org.springframework.security.authentication.UsernamePasswordAuthenticationToken(
                user, null, user.getAuthorities());
    }
}
