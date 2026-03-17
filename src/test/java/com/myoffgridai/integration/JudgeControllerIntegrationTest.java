package com.myoffgridai.integration;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.myoffgridai.ai.judge.JudgeInferenceService;
import com.myoffgridai.ai.judge.JudgeModelProcessService;
import com.myoffgridai.ai.judge.JudgeResult;
import com.myoffgridai.ai.service.OllamaService;
import com.myoffgridai.auth.dto.AuthResponse;
import com.myoffgridai.auth.dto.RegisterRequest;
import com.myoffgridai.auth.model.Role;
import com.myoffgridai.common.response.ApiResponse;
import com.myoffgridai.memory.service.EmbeddingService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import java.util.Optional;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Integration tests for the {@code /api/ai/judge} endpoints.
 *
 * <p>Verifies status, start, stop, and test endpoints with proper
 * authentication and role-based access control.</p>
 */
class JudgeControllerIntegrationTest extends BaseIntegrationTest {

    @Autowired private MockMvc mockMvc;
    @Autowired private ObjectMapper objectMapper;

    @MockitoBean private OllamaService ollamaService;
    @MockitoBean private EmbeddingService embeddingService;
    @MockitoBean private JudgeModelProcessService judgeModelProcessService;
    @MockitoBean private JudgeInferenceService judgeInferenceService;

    private String ownerToken;

    @BeforeEach
    void setUp() throws Exception {
        when(ollamaService.isAvailable()).thenReturn(false);

        // Register the first user (becomes OWNER automatically)
        ownerToken = registerOwner();

        // Mock judge process defaults
        when(judgeModelProcessService.isRunning()).thenReturn(false);
        when(judgeModelProcessService.getPort()).thenReturn(1235);
    }

    // ── GET /status ─────────────────────────────────────────────────────────

    @Test
    void getStatus_returnsJudgeStatus() throws Exception {
        mockMvc.perform(get("/api/ai/judge/status")
                        .header("Authorization", "Bearer " + ownerToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.processRunning").value(false))
                .andExpect(jsonPath("$.data.port").value(1235));
    }

    @Test
    void getStatus_requiresAuthentication() throws Exception {
        mockMvc.perform(get("/api/ai/judge/status"))
                .andExpect(status().isUnauthorized());
    }

    // ── POST /start ─────────────────────────────────────────────────────────

    @Test
    void start_callsProcessService() throws Exception {
        mockMvc.perform(post("/api/ai/judge/start")
                        .header("Authorization", "Bearer " + ownerToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.port").value(1235));

        verify(judgeModelProcessService).start();
    }

    // ── POST /stop ──────────────────────────────────────────────────────────

    @Test
    void stop_callsProcessService() throws Exception {
        mockMvc.perform(post("/api/ai/judge/stop")
                        .header("Authorization", "Bearer " + ownerToken))
                .andExpect(status().isOk());

        verify(judgeModelProcessService).stop();
    }

    // ── POST /test ──────────────────────────────────────────────────────────

    @Test
    void test_returnsUnavailableWhenJudgeNotReady() throws Exception {
        when(judgeInferenceService.isAvailable()).thenReturn(false);

        mockMvc.perform(post("/api/ai/judge/test")
                        .header("Authorization", "Bearer " + ownerToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"query\":\"What is Java?\",\"response\":\"A language\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.judgeAvailable").value(false))
                .andExpect(jsonPath("$.data.error").value("Judge is not available"));
    }

    @Test
    void test_returnsResultWhenAvailable() throws Exception {
        when(judgeInferenceService.isAvailable()).thenReturn(true);
        when(judgeInferenceService.evaluate(anyString(), anyString()))
                .thenReturn(Optional.of(new JudgeResult(8.0, "Good response", false)));

        mockMvc.perform(post("/api/ai/judge/test")
                        .header("Authorization", "Bearer " + ownerToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"query\":\"What is Java?\",\"response\":\"Java is a programming language\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.score").value(8.0))
                .andExpect(jsonPath("$.data.reason").value("Good response"))
                .andExpect(jsonPath("$.data.needsCloud").value(false))
                .andExpect(jsonPath("$.data.judgeAvailable").value(true));
    }

    @Test
    void test_handlesEmptyResult() throws Exception {
        when(judgeInferenceService.isAvailable()).thenReturn(true);
        when(judgeInferenceService.evaluate(anyString(), anyString()))
                .thenReturn(Optional.empty());

        mockMvc.perform(post("/api/ai/judge/test")
                        .header("Authorization", "Bearer " + ownerToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"query\":\"q\",\"response\":\"r\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.judgeAvailable").value(true))
                .andExpect(jsonPath("$.data.error").exists());
    }

    @Test
    void test_requiresValidBody() throws Exception {
        mockMvc.perform(post("/api/ai/judge/test")
                        .header("Authorization", "Bearer " + ownerToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"query\":\"\",\"response\":\"\"}"))
                .andExpect(status().isBadRequest());
    }

    // ── Helpers ─────────────────────────────────────────────────────────────

    private String registerOwner() throws Exception {
        String username = "owner_" + System.nanoTime();
        RegisterRequest register = new RegisterRequest(username, null, username, "password", null);

        MvcResult result = mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(register)))
                .andExpect(status().isCreated())
                .andReturn();

        String responseBody = result.getResponse().getContentAsString();
        ApiResponse<AuthResponse> response = objectMapper.readValue(responseBody,
                objectMapper.getTypeFactory().constructParametricType(
                        ApiResponse.class, AuthResponse.class));
        return response.getData().accessToken();
    }
}
