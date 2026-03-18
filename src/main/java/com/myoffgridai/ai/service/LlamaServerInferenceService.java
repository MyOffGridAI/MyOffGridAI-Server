package com.myoffgridai.ai.service;

import com.myoffgridai.ai.dto.ChunkType;
import com.myoffgridai.ai.dto.InferenceChunk;
import com.myoffgridai.ai.dto.InferenceMetadata;
import com.myoffgridai.ai.dto.InferenceModelInfo;
import com.myoffgridai.ai.dto.OllamaMessage;
import com.myoffgridai.common.exception.EmbeddingException;
import com.myoffgridai.common.exception.OllamaInferenceException;
import com.myoffgridai.config.AppConstants;
import com.myoffgridai.config.LlamaServerProperties;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Primary;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.MediaType;
import org.springframework.http.codec.ServerSentEvent;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Flux;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.atomic.AtomicReference;

/**
 * Inference provider implementation that calls the llama-server HTTP API
 * (OpenAI-compatible endpoints).
 *
 * <p>Delegates to the Homebrew-installed {@code llama-server} process managed
 * by {@link LlamaServerProcessService}. Supports synchronous and streaming
 * chat completions, embedding generation, model listing, and health checks.</p>
 *
 * <p>Streaming chat with thinking support detects {@code <think>}/{@code </think>}
 * tags in the SSE token stream and emits typed {@link InferenceChunk} instances,
 * using a think-tag detection state machine.</p>
 *
 * <p>Activated when {@code app.inference.provider=llama-server}.</p>
 */
@Service
@Primary
@ConditionalOnProperty(name = "app.inference.provider", havingValue = "llama-server")
public class LlamaServerInferenceService implements InferenceService {

    private static final Logger log = LoggerFactory.getLogger(LlamaServerInferenceService.class);

    private final LlamaServerProperties properties;
    private final RestClient restClient;
    private final WebClient webClient;

    /**
     * Constructs the llama-server inference service.
     *
     * @param properties llama-server configuration
     * @param restClient blocking REST client for synchronous API calls
     * @param webClient  reactive WebClient for SSE streaming
     */
    public LlamaServerInferenceService(LlamaServerProperties properties,
                                        @Qualifier("llamaServerRestClient") RestClient restClient,
                                        @Qualifier("llamaServerWebClient") WebClient webClient) {
        this.properties = properties;
        this.restClient = restClient;
        this.webClient = webClient;
    }

    /**
     * Checks whether the llama-server is reachable and healthy.
     *
     * @return true if the health endpoint returns HTTP 200
     */
    @Override
    public boolean isAvailable() {
        try {
            restClient.get()
                    .uri("/health")
                    .retrieve()
                    .toBodilessEntity();
            return true;
        } catch (Exception e) {
            log.warn("llama-server health check failed: {}", e.getMessage());
            return false;
        }
    }

    /**
     * Lists models available on the llama-server.
     *
     * @return list of model info records, or empty list on connection error
     */
    @Override
    @SuppressWarnings("unchecked")
    public List<InferenceModelInfo> listModels() {
        try {
            Map<String, Object> response = restClient.get()
                    .uri(AppConstants.LLAMA_SERVER_MODELS_ENDPOINT)
                    .retrieve()
                    .body(new ParameterizedTypeReference<>() {});

            if (response == null || !response.containsKey("data")) {
                return List.of();
            }

            List<Map<String, Object>> data = (List<Map<String, Object>>) response.get("data");
            return data.stream()
                    .map(m -> new InferenceModelInfo(
                            (String) m.getOrDefault("id", "unknown"),
                            (String) m.getOrDefault("id", "unknown"),
                            null,
                            "gguf",
                            null))
                    .toList();
        } catch (Exception e) {
            log.warn("Failed to list models from llama-server: {}", e.getMessage());
            return List.of();
        }
    }

    /**
     * Returns the currently active model from the llama-server.
     *
     * <p>Calls {@link #listModels()} and returns the first result. Falls back
     * to constructing an {@link InferenceModelInfo} from the configured
     * active model filename.</p>
     *
     * @return the active model info
     */
    @Override
    public InferenceModelInfo getActiveModel() {
        List<InferenceModelInfo> models = listModels();
        if (!models.isEmpty()) {
            return models.getFirst();
        }
        String fallback = properties.getActiveModel();
        return new InferenceModelInfo(
                fallback != null ? fallback : "unknown",
                fallback != null ? fallback : "unknown",
                null, "gguf", null);
    }

    /**
     * Sends a synchronous chat completion request to llama-server.
     *
     * @param messages the conversation history
     * @param userId   the requesting user's ID
     * @return the assistant's complete response text
     * @throws OllamaInferenceException if the request fails
     */
    @Override
    @SuppressWarnings("unchecked")
    public String chat(List<OllamaMessage> messages, UUID userId) {
        log.debug("llama-server sync chat for user {}", userId);

        Map<String, Object> requestBody = Map.of(
                "model", "local",
                "messages", messages.stream()
                        .map(m -> Map.of("role", m.role(), "content", m.content()))
                        .toList(),
                "stream", false,
                "temperature", 0.7
        );

        try {
            Map<String, Object> response = restClient.post()
                    .uri(AppConstants.LLAMA_SERVER_CHAT_ENDPOINT)
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(requestBody)
                    .retrieve()
                    .body(new ParameterizedTypeReference<>() {});

            if (response == null || !response.containsKey("choices")) {
                throw new OllamaInferenceException("llama-server returned empty response");
            }

            List<Map<String, Object>> choices = (List<Map<String, Object>>) response.get("choices");
            if (choices.isEmpty()) {
                throw new OllamaInferenceException("llama-server returned no choices");
            }

            Map<String, Object> message = (Map<String, Object>) choices.getFirst().get("message");
            return (String) message.get("content");

        } catch (OllamaInferenceException e) {
            throw e;
        } catch (Exception e) {
            throw new OllamaInferenceException("llama-server chat failed: " + e.getMessage(), e);
        }
    }

    /**
     * Sends a streaming chat completion request. Emits content tokens as strings.
     *
     * @param messages the conversation history
     * @param userId   the requesting user's ID
     * @return a reactive stream of content token strings
     */
    @Override
    public Flux<String> streamChat(List<OllamaMessage> messages, UUID userId) {
        return streamChatWithThinking(messages, userId)
                .filter(chunk -> chunk.type() == ChunkType.CONTENT)
                .map(InferenceChunk::text);
    }

    /**
     * Sends a streaming chat completion request with think-tag detection.
     *
     * <p>Parses the SSE token stream for {@code <think>}/{@code </think>} tags
     * and emits {@link ChunkType#THINKING} chunks for reasoning traces and
     * {@link ChunkType#CONTENT} chunks for user-facing content. A terminal
     * {@link ChunkType#DONE} chunk carries inference performance metadata.</p>
     *
     * @param messages the conversation history
     * @param userId   the requesting user's ID
     * @return a reactive stream of typed inference chunks
     */
    @Override
    public Flux<InferenceChunk> streamChatWithThinking(List<OllamaMessage> messages, UUID userId) {
        log.debug("llama-server streaming chat with thinking for user {}", userId);

        Map<String, Object> requestBody = Map.of(
                "model", "local",
                "messages", messages.stream()
                        .map(m -> Map.of("role", m.role(), "content", m.content()))
                        .toList(),
                "stream", true,
                "temperature", 0.7
        );

        AtomicReference<ThinkState> state = new AtomicReference<>(ThinkState.OUTSIDE_THINK);
        StringBuilder tagBuffer = new StringBuilder();
        AtomicLong startNanos = new AtomicLong(System.nanoTime());
        AtomicInteger tokenCount = new AtomicInteger(0);

        return webClient.post()
                .uri(AppConstants.LLAMA_SERVER_CHAT_ENDPOINT)
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(requestBody)
                .retrieve()
                .bodyToFlux(String.class)
                .filter(line -> line.startsWith("data: "))
                .map(line -> line.substring(6).trim())
                .filter(data -> !data.equals("[DONE]"))
                .concatMap(data -> {
                    String token = extractDeltaContent(data);
                    if (token == null || token.isEmpty()) {
                        return Flux.empty();
                    }
                    tokenCount.incrementAndGet();
                    return processToken(token, state, tagBuffer);
                })
                .concatWith(Flux.defer(() -> {
                    List<InferenceChunk> remaining = flushBuffer(tagBuffer, state.get());
                    long endNanos = System.nanoTime();
                    double inferenceTime = (endNanos - startNanos.get()) / 1e9;
                    int tokens = tokenCount.get();
                    double tokPerSec = inferenceTime > 0 ? tokens / inferenceTime : 0;

                    List<InferenceChunk> result = new java.util.ArrayList<>(remaining);
                    result.add(new InferenceChunk(
                            ChunkType.DONE, null,
                            new InferenceMetadata(tokens, tokPerSec, inferenceTime, "stop")));
                    return Flux.fromIterable(result);
                }));
    }

    /**
     * Generates an embedding vector for the given text using the llama-server
     * embeddings endpoint.
     *
     * @param text the text to embed
     * @return the embedding vector as a float array
     * @throws EmbeddingException if the request fails
     */
    @Override
    @SuppressWarnings("unchecked")
    public float[] embed(String text) {
        log.debug("Embedding via llama-server");

        Map<String, Object> requestBody = Map.of(
                "model", "local",
                "input", text
        );

        try {
            Map<String, Object> response = restClient.post()
                    .uri("/v1/embeddings")
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(requestBody)
                    .retrieve()
                    .body(new ParameterizedTypeReference<>() {});

            if (response == null || !response.containsKey("data")) {
                throw new EmbeddingException("llama-server embedding returned no data");
            }

            List<Map<String, Object>> data = (List<Map<String, Object>>) response.get("data");
            if (data.isEmpty()) {
                throw new EmbeddingException("llama-server embedding returned empty data array");
            }

            List<Number> embedding = (List<Number>) data.getFirst().get("embedding");
            float[] result = new float[embedding.size()];
            for (int i = 0; i < embedding.size(); i++) {
                result[i] = embedding.get(i).floatValue();
            }
            return result;

        } catch (EmbeddingException e) {
            throw e;
        } catch (Exception e) {
            throw new EmbeddingException("llama-server embedding failed: " + e.getMessage(), e);
        }
    }

    // ── Think-tag state machine ──────────────────────────────────────────

    /**
     * State machine states for think-tag parsing.
     */
    private enum ThinkState {
        OUTSIDE_THINK,
        INSIDE_THINK
    }

    @SuppressWarnings("unchecked")
    private String extractDeltaContent(String json) {
        try {
            var objectMapper = new com.fasterxml.jackson.databind.ObjectMapper();
            Map<String, Object> parsed = objectMapper.readValue(json, Map.class);
            List<Map<String, Object>> choices = (List<Map<String, Object>>) parsed.get("choices");
            if (choices == null || choices.isEmpty()) {
                return null;
            }
            Map<String, Object> delta = (Map<String, Object>) choices.getFirst().get("delta");
            if (delta == null) {
                return null;
            }
            return (String) delta.get("content");
        } catch (Exception e) {
            return null;
        }
    }

    private Flux<InferenceChunk> processToken(
            String token,
            AtomicReference<ThinkState> state,
            StringBuilder tagBuffer) {

        List<InferenceChunk> chunks = new java.util.ArrayList<>();
        tagBuffer.append(token);

        while (tagBuffer.length() > 0) {
            String buf = tagBuffer.toString();

            if (state.get() == ThinkState.OUTSIDE_THINK) {
                int thinkIdx = buf.indexOf(AppConstants.THINK_TAG_OPEN);

                if (thinkIdx == -1) {
                    int partialLen = partialTagMatchLength(buf, AppConstants.THINK_TAG_OPEN);
                    if (partialLen > 0 && partialLen == buf.length()) {
                        break;
                    }
                    String safe = partialLen > 0 ? buf.substring(0, buf.length() - partialLen) : buf;
                    if (!safe.isEmpty()) {
                        chunks.add(new InferenceChunk(ChunkType.CONTENT, safe, null));
                    }
                    tagBuffer.setLength(0);
                    if (partialLen > 0) {
                        tagBuffer.append(buf.substring(buf.length() - partialLen));
                    }
                    break;
                }

                if (thinkIdx > 0) {
                    chunks.add(new InferenceChunk(ChunkType.CONTENT,
                            buf.substring(0, thinkIdx), null));
                }

                state.set(ThinkState.INSIDE_THINK);
                tagBuffer.setLength(0);
                String remaining = buf.substring(thinkIdx + AppConstants.THINK_TAG_OPEN.length());
                if (!remaining.isEmpty()) {
                    tagBuffer.append(remaining);
                }

            } else {
                int closeIdx = buf.indexOf(AppConstants.THINK_TAG_CLOSE);

                if (closeIdx == -1) {
                    int partialLen = partialTagMatchLength(buf, AppConstants.THINK_TAG_CLOSE);
                    if (partialLen > 0 && partialLen == buf.length()) {
                        break;
                    }
                    String safe = partialLen > 0 ? buf.substring(0, buf.length() - partialLen) : buf;
                    if (!safe.isEmpty()) {
                        chunks.add(new InferenceChunk(ChunkType.THINKING, safe, null));
                    }
                    tagBuffer.setLength(0);
                    if (partialLen > 0) {
                        tagBuffer.append(buf.substring(buf.length() - partialLen));
                    }
                    break;
                }

                if (closeIdx > 0) {
                    chunks.add(new InferenceChunk(ChunkType.THINKING,
                            buf.substring(0, closeIdx), null));
                }

                state.set(ThinkState.OUTSIDE_THINK);
                tagBuffer.setLength(0);
                String remaining = buf.substring(closeIdx + AppConstants.THINK_TAG_CLOSE.length());
                if (!remaining.isEmpty()) {
                    tagBuffer.append(remaining);
                }
            }
        }

        return Flux.fromIterable(chunks);
    }

    private List<InferenceChunk> flushBuffer(StringBuilder tagBuffer, ThinkState state) {
        if (tagBuffer.length() > 0) {
            ChunkType type = state == ThinkState.INSIDE_THINK ? ChunkType.THINKING : ChunkType.CONTENT;
            InferenceChunk chunk = new InferenceChunk(type, tagBuffer.toString(), null);
            tagBuffer.setLength(0);
            return List.of(chunk);
        }
        return List.of();
    }

    private static int partialTagMatchLength(String text, String tag) {
        int maxLen = Math.min(text.length(), tag.length() - 1);
        for (int len = maxLen; len >= 1; len--) {
            if (text.endsWith(tag.substring(0, len))) {
                return len;
            }
        }
        return 0;
    }
}
