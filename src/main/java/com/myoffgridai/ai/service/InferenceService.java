package com.myoffgridai.ai.service;

import com.myoffgridai.ai.dto.InferenceChunk;
import com.myoffgridai.ai.dto.InferenceModelInfo;
import com.myoffgridai.ai.dto.OllamaMessage;
import reactor.core.publisher.Flux;

import java.util.List;
import java.util.UUID;

/**
 * Abstraction over local LLM inference providers.
 *
 * <p>Current implementations:
 * <ul>
 *   <li>{@link OllamaInferenceService} — delegates to Ollama ({@code app.inference.provider=ollama})</li>
 *   <li>{@link LmStudioInferenceService} — LM Studio OpenAI-compatible API ({@code app.inference.provider=lmstudio})</li>
 * </ul>
 *
 * <p>The active implementation is selected via the {@code app.inference.provider} configuration
 * property using Spring {@code @ConditionalOnProperty}.
 */
public interface InferenceService {

    /**
     * Synchronous chat completion. Returns the full response text.
     *
     * @param messages the conversation history
     * @param userId   the requesting user's ID (for per-user config lookup)
     * @return the assistant's complete response text
     */
    String chat(List<OllamaMessage> messages, UUID userId);

    /**
     * Streaming chat completion. Emits content tokens as they arrive.
     *
     * @param messages the conversation history
     * @param userId   the requesting user's ID
     * @return a reactive stream of content token strings
     */
    Flux<String> streamChat(List<OllamaMessage> messages, UUID userId);

    /**
     * Streaming chat that also emits thinking tokens separately.
     *
     * <p>For providers that support reasoning models with {@code <think>} tags,
     * thinking content is emitted as {@link com.myoffgridai.ai.dto.ChunkType#THINKING} chunks
     * and response content as {@link com.myoffgridai.ai.dto.ChunkType#CONTENT} chunks.
     * A terminal {@link com.myoffgridai.ai.dto.ChunkType#DONE} chunk carries inference metadata.
     *
     * @param messages the conversation history
     * @param userId   the requesting user's ID
     * @return a reactive stream of typed inference chunks
     */
    Flux<InferenceChunk> streamChatWithThinking(List<OllamaMessage> messages, UUID userId);

    /**
     * Generate an embedding vector for the given text.
     *
     * @param text the text to embed
     * @return the embedding vector as a float array
     */
    float[] embed(String text);

    /**
     * Returns {@code true} if the inference provider is reachable and healthy.
     *
     * @return provider availability status
     */
    boolean isAvailable();

    /**
     * Lists all models available or loaded on the inference provider.
     *
     * @return list of model info records
     */
    List<InferenceModelInfo> listModels();

    /**
     * Returns the currently active model name and metadata.
     *
     * @return the active model info
     */
    InferenceModelInfo getActiveModel();
}
