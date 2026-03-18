package com.myoffgridai.ai.service;

import com.myoffgridai.ai.dto.ChunkType;
import com.myoffgridai.ai.dto.InferenceChunk;
import com.myoffgridai.ai.dto.InferenceMetadata;
import com.myoffgridai.ai.dto.InferenceModelInfo;
import com.myoffgridai.ai.dto.OllamaChatRequest;
import com.myoffgridai.ai.dto.OllamaChatResponse;
import com.myoffgridai.ai.dto.OllamaMessage;
import com.myoffgridai.ai.dto.OllamaModelInfo;
import com.myoffgridai.system.service.SystemConfigService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;

import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;

/**
 * Inference provider implementation that delegates to the existing {@link OllamaService}.
 *
 * <p>This is a thin wrapper that adapts OllamaService to the {@link InferenceService}
 * interface. Activated when {@code app.inference.provider=ollama}.
 *
 * <p>Reads the {@code thinking} field from Ollama's streaming response (available in
 * Ollama 0.6+) to classify streamed tokens as {@link ChunkType#THINKING} or
 * {@link ChunkType#CONTENT}.</p>
 */
@Service
@ConditionalOnProperty(name = "app.inference.provider", havingValue = "ollama", matchIfMissing = true)
public class OllamaInferenceService implements InferenceService {

    private static final Logger log = LoggerFactory.getLogger(OllamaInferenceService.class);

    private final OllamaService ollamaService;
    private final SystemConfigService systemConfigService;
    private final String modelName;
    private final String embedModelName;

    /**
     * Constructs the Ollama inference wrapper.
     *
     * @param ollamaService       the underlying Ollama service
     * @param systemConfigService system config for AI settings
     * @param modelName           configured Ollama model name
     * @param embedModelName      configured Ollama embed model name
     */
    public OllamaInferenceService(
            OllamaService ollamaService,
            SystemConfigService systemConfigService,
            @Qualifier("ollamaModelName") String modelName,
            @Qualifier("ollamaEmbedModelName") String embedModelName) {
        this.ollamaService = ollamaService;
        this.systemConfigService = systemConfigService;
        this.modelName = modelName;
        this.embedModelName = embedModelName;
    }

    /** {@inheritDoc} */
    @Override
    public String chat(List<OllamaMessage> messages, UUID userId) {
        log.debug("Ollama sync chat for user {}", userId);
        var aiSettings = systemConfigService.getAiSettings();
        var request = new OllamaChatRequest(
                aiSettings.modelName() != null ? aiSettings.modelName() : modelName,
                messages, false,
                Map.of("num_ctx", aiSettings.contextSize(),
                        "temperature", aiSettings.temperature()));
        OllamaChatResponse response = ollamaService.chat(request);
        return response.message().content();
    }

    /** {@inheritDoc} */
    @Override
    public Flux<String> streamChat(List<OllamaMessage> messages, UUID userId) {
        log.debug("Ollama streaming chat for user {}", userId);
        var aiSettings = systemConfigService.getAiSettings();
        var request = new OllamaChatRequest(
                aiSettings.modelName() != null ? aiSettings.modelName() : modelName,
                messages, true,
                Map.of("num_ctx", aiSettings.contextSize(),
                        "temperature", aiSettings.temperature()));
        return ollamaService.chatStream(request)
                .filter(chunk -> chunk.message() != null && chunk.message().content() != null
                        && !chunk.message().content().isEmpty())
                .map(chunk -> chunk.message().content());
    }

    /**
     * {@inheritDoc}
     *
     * <p>Reads the {@code thinking} field from each Ollama streaming chunk. When the
     * model is reasoning, chunks arrive with an empty {@code content} field and a
     * non-empty {@code thinking} field. Once reasoning completes, {@code content}
     * carries the visible response text.</p>
     */
    @Override
    public Flux<InferenceChunk> streamChatWithThinking(List<OllamaMessage> messages, UUID userId) {
        log.debug("Ollama streaming chat with thinking for user {}", userId);
        var aiSettings = systemConfigService.getAiSettings();
        var request = new OllamaChatRequest(
                aiSettings.modelName() != null ? aiSettings.modelName() : modelName,
                messages, true,
                Map.of("num_ctx", aiSettings.contextSize(),
                        "temperature", aiSettings.temperature()),
                true);

        AtomicLong startNanos = new AtomicLong(System.nanoTime());
        AtomicInteger tokenCount = new AtomicInteger(0);

        return ollamaService.chatStream(request)
                .handle((chunk, sink) -> {
                    if (chunk.done()) {
                        long endNanos = System.nanoTime();
                        double inferenceTime = (endNanos - startNanos.get()) / 1e9;
                        int tokens = tokenCount.get();
                        double tokPerSec = inferenceTime > 0 ? tokens / inferenceTime : 0;

                        sink.next(new InferenceChunk(ChunkType.DONE, null,
                                new InferenceMetadata(tokens, tokPerSec, inferenceTime, "stop")));
                        return;
                    }

                    if (chunk.message() == null) {
                        return;
                    }

                    // Check for thinking content (Ollama 0.6+ native thinking field)
                    String thinking = chunk.message().thinking();
                    if (thinking != null && !thinking.isEmpty()) {
                        tokenCount.incrementAndGet();
                        sink.next(new InferenceChunk(ChunkType.THINKING, thinking, null));
                        return;
                    }

                    // Check for content
                    String content = chunk.message().content();
                    if (content != null && !content.isEmpty()) {
                        tokenCount.incrementAndGet();
                        sink.next(new InferenceChunk(ChunkType.CONTENT, content, null));
                    }
                    // Chunks with both fields empty are silently skipped
                });
    }

    /** {@inheritDoc} */
    @Override
    public float[] embed(String text) {
        return ollamaService.embed(text);
    }

    /** {@inheritDoc} */
    @Override
    public boolean isAvailable() {
        return ollamaService.isAvailable();
    }

    /** {@inheritDoc} */
    @Override
    public List<InferenceModelInfo> listModels() {
        return ollamaService.listModels().stream()
                .map(m -> new InferenceModelInfo(
                        m.name(),
                        m.name(),
                        m.size(),
                        null,
                        m.modifiedAt()))
                .toList();
    }

    /** {@inheritDoc} */
    @Override
    public InferenceModelInfo getActiveModel() {
        return new InferenceModelInfo(modelName, modelName, null, null, null);
    }
}
