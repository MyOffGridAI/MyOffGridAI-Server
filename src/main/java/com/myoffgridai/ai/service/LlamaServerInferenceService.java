package com.myoffgridai.ai.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.myoffgridai.ai.dto.ChunkType;
import com.myoffgridai.ai.dto.InferenceChunk;
import com.myoffgridai.ai.dto.InferenceMetadata;
import com.myoffgridai.ai.dto.InferenceModelInfo;
import com.myoffgridai.ai.dto.OllamaMessage;
import com.myoffgridai.config.AppConstants;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Sinks;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.atomic.AtomicReference;

/**
 * Inference provider implementation for llama-server's OpenAI-compatible REST API.
 *
 * <p>llama-server (from llama.cpp) exposes endpoints matching the OpenAI specification:
 * {@code POST /v1/chat/completions} for chat, {@code GET /v1/models} for model listing.
 * Streaming uses Server-Sent Events with {@code data: {...}} lines terminated by
 * {@code data: [DONE]}.
 *
 * <p>Reasoning models (e.g. Qwen3.5 distilled) emit thinking traces wrapped in
 * {@code <think>...</think>} XML tags inline within the content stream. This service
 * detects and splits those tags, emitting {@link ChunkType#THINKING} chunks for
 * content inside the tags and {@link ChunkType#CONTENT} chunks for content outside.
 *
 * <p>Activated when {@code app.inference.provider=llama-server}.
 */
@Service
@ConditionalOnProperty(name = "app.inference.provider", havingValue = "llama-server")
public class LlamaServerInferenceService implements InferenceService {

    private static final Logger log = LoggerFactory.getLogger(LlamaServerInferenceService.class);

    private final WebClient llamaServerWebClient;
    private final RestClient llamaServerRestClient;
    private final RestClient ollamaEmbedRestClient;
    private final ObjectMapper objectMapper;

    @Value("${app.inference.model}")
    private String model;

    @Value("${app.inference.embed-model:nomic-embed-text}")
    private String embedModel;

    @Value("${app.inference.max-tokens:4096}")
    private int maxTokens;

    @Value("${app.inference.temperature:0.7}")
    private double temperature;

    /**
     * Constructs the llama-server inference service.
     *
     * @param llamaServerWebClient    reactive client pointed at the llama-server base URL
     * @param llamaServerRestClient   blocking client pointed at the llama-server base URL
     * @param ollamaEmbedRestClient   blocking client pointed at the Ollama base URL for embeddings
     * @param objectMapper            Jackson ObjectMapper for JSON parsing
     */
    public LlamaServerInferenceService(
            @Qualifier("llamaServerWebClient") WebClient llamaServerWebClient,
            @Qualifier("llamaServerRestClient") RestClient llamaServerRestClient,
            @Qualifier("ollamaEmbedRestClient") RestClient ollamaEmbedRestClient,
            ObjectMapper objectMapper) {
        this.llamaServerWebClient = llamaServerWebClient;
        this.llamaServerRestClient = llamaServerRestClient;
        this.ollamaEmbedRestClient = ollamaEmbedRestClient;
        this.objectMapper = objectMapper;
    }

    /** {@inheritDoc} */
    @Override
    public String chat(List<OllamaMessage> messages, UUID userId) {
        log.debug("llama-server sync chat for user {}", userId);

        Map<String, Object> requestBody = buildChatRequest(messages, false);

        Map<String, Object> response = llamaServerRestClient.post()
                .uri(AppConstants.LLAMA_SERVER_CHAT_ENDPOINT)
                .contentType(MediaType.APPLICATION_JSON)
                .body(requestBody)
                .retrieve()
                .body(new ParameterizedTypeReference<>() {});

        if (response == null) {
            throw new RuntimeException("llama-server returned null response");
        }

        @SuppressWarnings("unchecked")
        List<Map<String, Object>> choices = (List<Map<String, Object>>) response.get("choices");
        if (choices == null || choices.isEmpty()) {
            throw new RuntimeException("llama-server returned no choices");
        }

        @SuppressWarnings("unchecked")
        Map<String, Object> messageMap = (Map<String, Object>) choices.get(0).get("message");
        String content = (String) messageMap.get("content");

        // Strip any <think>...</think> tags from sync response
        return stripThinkTags(content);
    }

    /** {@inheritDoc} */
    @Override
    public Flux<String> streamChat(List<OllamaMessage> messages, UUID userId) {
        return streamChatWithThinking(messages, userId)
                .filter(chunk -> chunk.type() == ChunkType.CONTENT)
                .map(InferenceChunk::text);
    }

    /** {@inheritDoc} */
    @Override
    public Flux<InferenceChunk> streamChatWithThinking(List<OllamaMessage> messages, UUID userId) {
        log.debug("llama-server streaming chat with thinking for user {}", userId);

        Map<String, Object> requestBody = buildChatRequest(messages, true);

        // State machine for <think> tag parsing
        AtomicReference<ThinkState> state = new AtomicReference<>(ThinkState.OUTSIDE_THINK);
        StringBuilder tagBuffer = new StringBuilder();
        AtomicLong startNanos = new AtomicLong(0);
        AtomicLong endNanos = new AtomicLong(0);
        AtomicInteger completionTokens = new AtomicInteger(0);
        AtomicReference<String> stopReason = new AtomicReference<>(null);

        Sinks.Many<InferenceChunk> sink = Sinks.many().unicast().onBackpressureBuffer();

        llamaServerWebClient.post()
                .uri(AppConstants.LLAMA_SERVER_CHAT_ENDPOINT)
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(requestBody)
                .retrieve()
                .bodyToFlux(String.class)
                .doOnSubscribe(sub -> startNanos.set(System.nanoTime()))
                .subscribe(
                        line -> processSSELine(line, state, tagBuffer, startNanos, endNanos,
                                completionTokens, stopReason, sink),
                        error -> {
                            log.error("llama-server streaming error", error);
                            sink.tryEmitError(error);
                        },
                        () -> {
                            // Flush any remaining buffer content
                            flushBuffer(tagBuffer, state.get(), sink);

                            if (endNanos.get() == 0) {
                                endNanos.set(System.nanoTime());
                            }

                            double inferenceTime = (endNanos.get() - startNanos.get()) / 1e9;
                            int tokens = completionTokens.get();
                            double tokPerSec = inferenceTime > 0 ? tokens / inferenceTime : 0;

                            sink.tryEmitNext(new InferenceChunk(
                                    ChunkType.DONE, null,
                                    new InferenceMetadata(tokens, tokPerSec, inferenceTime,
                                            stopReason.get() != null ? stopReason.get() : "stop")));
                            sink.tryEmitComplete();
                        });

        return sink.asFlux();
    }

    /** {@inheritDoc} */
    @Override
    public float[] embed(String text) {
        log.debug("Embedding via Ollama (embed-model: {})", embedModel);

        Map<String, Object> requestBody = Map.of(
                "model", embedModel,
                "prompt", text);

        Map<String, Object> response = ollamaEmbedRestClient.post()
                .uri("/api/embeddings")
                .contentType(MediaType.APPLICATION_JSON)
                .body(requestBody)
                .retrieve()
                .body(new ParameterizedTypeReference<>() {});

        if (response == null || !response.containsKey("embedding")) {
            throw new RuntimeException("Ollama embed returned no embedding");
        }

        @SuppressWarnings("unchecked")
        List<Number> embedding = (List<Number>) response.get("embedding");
        float[] result = new float[embedding.size()];
        for (int i = 0; i < embedding.size(); i++) {
            result[i] = embedding.get(i).floatValue();
        }
        return result;
    }

    /** {@inheritDoc} */
    @Override
    public boolean isAvailable() {
        try {
            llamaServerRestClient.get()
                    .uri(AppConstants.LLAMA_SERVER_MODELS_ENDPOINT)
                    .retrieve()
                    .body(String.class);
            return true;
        } catch (Exception e) {
            log.warn("llama-server unavailable: {}", e.getMessage());
            return false;
        }
    }

    /** {@inheritDoc} */
    @Override
    public List<InferenceModelInfo> listModels() {
        try {
            Map<String, Object> response = llamaServerRestClient.get()
                    .uri(AppConstants.LLAMA_SERVER_MODELS_ENDPOINT)
                    .retrieve()
                    .body(new ParameterizedTypeReference<>() {});

            if (response == null || !response.containsKey("data")) {
                return List.of();
            }

            @SuppressWarnings("unchecked")
            List<Map<String, Object>> data = (List<Map<String, Object>>) response.get("data");
            List<InferenceModelInfo> models = new ArrayList<>();
            for (Map<String, Object> modelData : data) {
                String id = (String) modelData.get("id");
                Long created = modelData.get("created") instanceof Number n ? n.longValue() : null;
                models.add(new InferenceModelInfo(
                        id,
                        id,
                        null,
                        "gguf",
                        created != null ? Instant.ofEpochSecond(created) : null));
            }
            return models;
        } catch (Exception e) {
            log.error("Failed to list llama-server models", e);
            return List.of();
        }
    }

    /** {@inheritDoc} */
    @Override
    public InferenceModelInfo getActiveModel() {
        List<InferenceModelInfo> models = listModels();
        if (!models.isEmpty()) {
            return models.get(0);
        }
        return new InferenceModelInfo(model, model, null, "gguf", null);
    }

    // ──────────────────────────────────────────────────────────────
    //  Private helpers
    // ──────────────────────────────────────────────────────────────

    private Map<String, Object> buildChatRequest(List<OllamaMessage> messages, boolean stream) {
        List<Map<String, String>> msgs = messages.stream()
                .map(m -> Map.of("role", m.role(), "content", m.content()))
                .toList();

        return Map.of(
                "model", model,
                "messages", msgs,
                "stream", stream,
                "max_tokens", maxTokens,
                "temperature", temperature);
    }

    /**
     * Processes a single SSE line from llama-server.
     * Handles the "data: " prefix and JSON parsing, then feeds content tokens
     * through the think-tag state machine.
     */
    private void processSSELine(
            String line,
            AtomicReference<ThinkState> state,
            StringBuilder tagBuffer,
            AtomicLong startNanos,
            AtomicLong endNanos,
            AtomicInteger completionTokens,
            AtomicReference<String> stopReason,
            Sinks.Many<InferenceChunk> sink) {

        String trimmed = line.trim();

        // Strip SSE "data: " prefix
        if (trimmed.startsWith("data: ")) {
            trimmed = trimmed.substring(6);
        } else if (trimmed.startsWith("data:")) {
            trimmed = trimmed.substring(5);
        }

        if (trimmed.isEmpty() || "[DONE]".equals(trimmed)) {
            return;
        }

        try {
            JsonNode node = objectMapper.readTree(trimmed);
            JsonNode choices = node.get("choices");
            if (choices == null || choices.isEmpty()) {
                return;
            }

            JsonNode firstChoice = choices.get(0);
            JsonNode delta = firstChoice.get("delta");
            JsonNode finishReason = firstChoice.get("finish_reason");

            // Extract content token
            if (delta != null && delta.has("content") && !delta.get("content").isNull()) {
                String content = delta.get("content").asText();
                if (!content.isEmpty()) {
                    processContentToken(content, state, tagBuffer, sink);
                }
            }

            // Extract finish reason and usage
            if (finishReason != null && !finishReason.isNull()) {
                endNanos.set(System.nanoTime());
                stopReason.set(finishReason.asText());
            }

            JsonNode usage = node.get("usage");
            if (usage != null && !usage.isNull() && usage.has("completion_tokens")) {
                completionTokens.set(usage.get("completion_tokens").asInt());
            }

        } catch (Exception e) {
            log.trace("Failed to parse SSE line: {}", trimmed, e);
        }
    }

    /**
     * Think-tag state machine. Accumulates characters in a buffer to detect
     * {@code <think>} and {@code </think>} tag boundaries even when they are
     * split across multiple SSE chunks.
     */
    private void processContentToken(
            String token,
            AtomicReference<ThinkState> state,
            StringBuilder tagBuffer,
            Sinks.Many<InferenceChunk> sink) {

        tagBuffer.append(token);

        while (tagBuffer.length() > 0) {
            String buf = tagBuffer.toString();

            if (state.get() == ThinkState.OUTSIDE_THINK) {
                int thinkIdx = buf.indexOf(AppConstants.THINK_TAG_OPEN);

                if (thinkIdx == -1) {
                    // Check for partial tag at end of buffer
                    int partialLen = partialTagMatchLength(buf, AppConstants.THINK_TAG_OPEN);
                    if (partialLen > 0 && partialLen == buf.length()) {
                        // Entire buffer is a partial tag — wait for more data
                        return;
                    }
                    // Emit everything except potential partial match at the end
                    String safe = partialLen > 0 ? buf.substring(0, buf.length() - partialLen) : buf;
                    if (!safe.isEmpty()) {
                        sink.tryEmitNext(new InferenceChunk(ChunkType.CONTENT, safe, null));
                    }
                    tagBuffer.setLength(0);
                    if (partialLen > 0) {
                        tagBuffer.append(buf.substring(buf.length() - partialLen));
                    }
                    return;
                }

                // Emit content before the tag
                if (thinkIdx > 0) {
                    sink.tryEmitNext(new InferenceChunk(ChunkType.CONTENT,
                            buf.substring(0, thinkIdx), null));
                }

                // Consume the opening tag
                state.set(ThinkState.INSIDE_THINK);
                tagBuffer.setLength(0);
                String remaining = buf.substring(thinkIdx + AppConstants.THINK_TAG_OPEN.length());
                if (!remaining.isEmpty()) {
                    tagBuffer.append(remaining);
                }
                // Continue loop to process remaining content

            } else {
                // INSIDE_THINK
                int closeIdx = buf.indexOf(AppConstants.THINK_TAG_CLOSE);

                if (closeIdx == -1) {
                    // Check for partial close tag
                    int partialLen = partialTagMatchLength(buf, AppConstants.THINK_TAG_CLOSE);
                    if (partialLen > 0 && partialLen == buf.length()) {
                        return;
                    }
                    String safe = partialLen > 0 ? buf.substring(0, buf.length() - partialLen) : buf;
                    if (!safe.isEmpty()) {
                        sink.tryEmitNext(new InferenceChunk(ChunkType.THINKING, safe, null));
                    }
                    tagBuffer.setLength(0);
                    if (partialLen > 0) {
                        tagBuffer.append(buf.substring(buf.length() - partialLen));
                    }
                    return;
                }

                // Emit thinking content before the close tag
                if (closeIdx > 0) {
                    sink.tryEmitNext(new InferenceChunk(ChunkType.THINKING,
                            buf.substring(0, closeIdx), null));
                }

                // Consume the closing tag
                state.set(ThinkState.OUTSIDE_THINK);
                tagBuffer.setLength(0);
                String remaining = buf.substring(closeIdx + AppConstants.THINK_TAG_CLOSE.length());
                if (!remaining.isEmpty()) {
                    tagBuffer.append(remaining);
                }
                // Continue loop to process remaining content
            }
        }
    }

    /**
     * Flushes any remaining content in the tag buffer.
     */
    private void flushBuffer(StringBuilder tagBuffer, ThinkState state,
                             Sinks.Many<InferenceChunk> sink) {
        if (tagBuffer.length() > 0) {
            ChunkType type = state == ThinkState.INSIDE_THINK ? ChunkType.THINKING : ChunkType.CONTENT;
            sink.tryEmitNext(new InferenceChunk(type, tagBuffer.toString(), null));
            tagBuffer.setLength(0);
        }
    }

    /**
     * Returns the length of a partial match of {@code tag} at the end of {@code text}.
     * For example, if text ends with "{@code <thi}" and tag is "{@code <think>}",
     * this returns 4 because the last 4 characters match the first 4 of the tag.
     */
    static int partialTagMatchLength(String text, String tag) {
        int maxLen = Math.min(text.length(), tag.length() - 1);
        for (int len = maxLen; len >= 1; len--) {
            if (text.endsWith(tag.substring(0, len))) {
                return len;
            }
        }
        return 0;
    }

    /**
     * Strips {@code <think>...</think>} blocks from content for synchronous responses.
     */
    private String stripThinkTags(String content) {
        if (content == null) {
            return "";
        }
        return content.replaceAll("(?s)<think>.*?</think>", "").trim();
    }

    /**
     * State machine states for think-tag parsing.
     */
    enum ThinkState {
        OUTSIDE_THINK,
        INSIDE_THINK
    }
}
