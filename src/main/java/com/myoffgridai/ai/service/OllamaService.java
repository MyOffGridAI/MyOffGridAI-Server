package com.myoffgridai.ai.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.myoffgridai.ai.dto.*;
import com.myoffgridai.common.exception.OllamaInferenceException;
import com.myoffgridai.common.exception.OllamaUnavailableException;
import com.myoffgridai.config.AppConstants;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Flux;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Sole integration point with the Ollama LLM service.
 *
 * <p>All other services must call Ollama through this class — never directly.
 * Provides synchronous chat, streaming chat, embedding, model listing,
 * and health checking methods.</p>
 */
@Service
public class OllamaService {

    private static final Logger log = LoggerFactory.getLogger(OllamaService.class);

    private final RestClient restClient;
    private final WebClient webClient;
    private final ObjectMapper objectMapper;

    /**
     * Constructs the OllamaService with configured HTTP clients.
     *
     * @param restClient   the blocking REST client for Ollama
     * @param webClient    the reactive WebClient for streaming
     * @param objectMapper the Jackson object mapper for NDJSON parsing
     */
    public OllamaService(@Qualifier("ollamaRestClient") RestClient restClient,
                          @Qualifier("ollamaWebClient") WebClient webClient,
                          ObjectMapper objectMapper) {
        this.restClient = restClient;
        this.webClient = webClient;
        this.objectMapper = objectMapper;
    }

    /**
     * Checks if Ollama is available and responding.
     *
     * @return true if Ollama responds with 200, false otherwise
     */
    public boolean isAvailable() {
        try {
            restClient.get()
                    .uri("/api/tags")
                    .retrieve()
                    .body(String.class);
            return true;
        } catch (Exception e) {
            log.debug("Ollama availability check failed: {}", e.getMessage());
            return false;
        }
    }

    /**
     * Lists all models currently loaded in Ollama.
     *
     * @return list of available model information
     * @throws OllamaUnavailableException if Ollama is unreachable
     */
    @SuppressWarnings("unchecked")
    public List<OllamaModelInfo> listModels() {
        log.debug("Listing Ollama models");
        try {
            Map<String, Object> response = restClient.get()
                    .uri("/api/tags")
                    .retrieve()
                    .body(new ParameterizedTypeReference<>() {});

            if (response == null || !response.containsKey("models")) {
                return List.of();
            }

            List<Map<String, Object>> models = (List<Map<String, Object>>) response.get("models");
            List<OllamaModelInfo> result = new ArrayList<>();

            for (Map<String, Object> model : models) {
                String name = (String) model.get("name");
                long size = model.get("size") instanceof Number n ? n.longValue() : 0L;
                Instant modifiedAt = model.get("modified_at") != null
                        ? Instant.parse((String) model.get("modified_at"))
                        : null;
                result.add(new OllamaModelInfo(name, size, modifiedAt));
            }

            log.debug("Found {} Ollama models", result.size());
            return result;
        } catch (OllamaUnavailableException e) {
            throw e;
        } catch (Exception e) {
            throw new OllamaUnavailableException("Failed to list Ollama models", e);
        }
    }

    /**
     * Sends a synchronous chat request to Ollama and waits for the full response.
     *
     * @param request the chat request containing model, messages, and options
     * @return the complete chat response from Ollama
     * @throws OllamaUnavailableException if Ollama is unreachable
     * @throws OllamaInferenceException   if Ollama returns an error during inference
     */
    public OllamaChatResponse chat(OllamaChatRequest request) {
        log.debug("Sending synchronous chat request to model: {}", request.model());
        try {
            OllamaChatResponse response = restClient.post()
                    .uri("/api/chat")
                    .body(request)
                    .retrieve()
                    .body(OllamaChatResponse.class);

            if (response == null) {
                throw new OllamaInferenceException("Ollama returned null response");
            }

            log.debug("Received chat response, evalCount: {}", response.evalCount());
            return response;
        } catch (OllamaUnavailableException | OllamaInferenceException e) {
            throw e;
        } catch (Exception e) {
            if (isConnectionError(e)) {
                throw new OllamaUnavailableException("Ollama is not available for chat", e);
            }
            throw new OllamaInferenceException("Chat inference failed: " + e.getMessage(), e);
        }
    }

    /**
     * Sends a streaming chat request to Ollama, returning a reactive stream of token chunks.
     *
     * <p>Each chunk contains a partial message with the next token. The stream completes
     * when a chunk with {@code done: true} is received.</p>
     *
     * @param request the chat request (stream field is overridden to true)
     * @return a Flux emitting chunks as they arrive from Ollama
     */
    public Flux<OllamaChatChunk> chatStream(OllamaChatRequest request) {
        log.debug("Starting streaming chat request to model: {}", request.model());

        OllamaChatRequest streamRequest = new OllamaChatRequest(
                request.model(), request.messages(), true, request.options(), request.think());

        return webClient.post()
                .uri("/api/chat")
                .bodyValue(streamRequest)
                .retrieve()
                .bodyToFlux(String.class)
                .map(raw -> {
                    try {
                        return objectMapper.readValue(raw, OllamaChatChunk.class);
                    } catch (Exception e) {
                        log.warn("Failed to parse Ollama chunk: {}", e.getMessage());
                        return new OllamaChatChunk(null, false);
                    }
                });
    }

    /**
     * Generates an embedding vector for the given text.
     *
     * @param text the text to embed
     * @return the embedding vector as a float array
     * @throws OllamaUnavailableException if Ollama is unreachable
     */
    @SuppressWarnings("unchecked")
    public float[] embed(String text) {
        log.debug("Generating embedding for text of length: {}", text.length());
        try {
            Map<String, Object> request = Map.of(
                    "model", AppConstants.OLLAMA_EMBED_MODEL,
                    "prompt", text
            );

            Map<String, Object> response = restClient.post()
                    .uri("/api/embeddings")
                    .body(request)
                    .retrieve()
                    .body(new ParameterizedTypeReference<>() {});

            if (response == null || !response.containsKey("embedding")) {
                throw new OllamaInferenceException("Ollama returned no embedding");
            }

            List<Number> embedding = (List<Number>) response.get("embedding");
            float[] result = new float[embedding.size()];
            for (int i = 0; i < embedding.size(); i++) {
                result[i] = embedding.get(i).floatValue();
            }

            log.debug("Generated embedding with {} dimensions", result.length);
            return result;
        } catch (OllamaUnavailableException | OllamaInferenceException e) {
            throw e;
        } catch (Exception e) {
            throw new OllamaUnavailableException("Failed to generate embedding", e);
        }
    }

    /**
     * Generates embeddings for a batch of texts sequentially.
     *
     * <p>Ollama does not support true batch embedding, so each text is
     * embedded individually in order.</p>
     *
     * @param texts the list of texts to embed
     * @return a list of embedding vectors in the same order as input
     * @throws OllamaUnavailableException if Ollama is unreachable
     */
    public List<float[]> embedBatch(List<String> texts) {
        log.debug("Generating batch embeddings for {} texts", texts.size());
        List<float[]> results = new ArrayList<>();
        for (String text : texts) {
            results.add(embed(text));
        }
        return results;
    }

    private boolean isConnectionError(Exception e) {
        String message = e.getMessage();
        if (message == null) return false;
        return message.contains("Connection refused")
                || message.contains("connect timed out")
                || message.contains("I/O error");
    }
}
