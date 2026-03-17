package com.myoffgridai.ai.service;

import com.myoffgridai.ai.dto.ChunkType;
import com.myoffgridai.ai.dto.InferenceChunk;
import com.myoffgridai.ai.dto.InferenceMetadata;
import com.myoffgridai.ai.dto.InferenceModelInfo;
import com.myoffgridai.ai.dto.NativeLlamaStatusDto;
import com.myoffgridai.ai.dto.OllamaMessage;
import com.myoffgridai.config.AppConstants;
import com.myoffgridai.config.InferenceProperties;
import com.myoffgridai.config.NativeLlamaProperties;
import com.myoffgridai.system.service.SystemConfigService;
import de.kherud.llama.InferenceParameters;
import de.kherud.llama.LlamaModel;
import de.kherud.llama.LlamaOutput;
import de.kherud.llama.ModelParameters;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.DisposableBean;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Sinks;
import reactor.core.scheduler.Schedulers;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.atomic.AtomicReference;

/**
 * Native in-process inference provider using java-llama.cpp JNI bindings.
 *
 * <p>Loads GGUF models directly into the JVM process via
 * {@link de.kherud.llama.LlamaModel}, eliminating the need for an external
 * llama-server binary. Supports synchronous and streaming chat with
 * think-tag parsing for reasoning models.</p>
 *
 * <p>Reasoning models (e.g. Qwen3.5 distilled) emit thinking traces wrapped in
 * {@code <think>...</think>} XML tags inline within the content stream. This service
 * detects and splits those tags, emitting {@link ChunkType#THINKING} chunks for
 * content inside the tags and {@link ChunkType#CONTENT} chunks for content outside.</p>
 *
 * <p>Activated when {@code app.inference.provider=native}.</p>
 */
@Service
@ConditionalOnProperty(name = "app.inference.provider", havingValue = "native")
public class NativeLlamaInferenceService implements InferenceService, DisposableBean {

    private static final Logger log = LoggerFactory.getLogger(NativeLlamaInferenceService.class);

    private final InferenceProperties inferenceProperties;
    private final NativeLlamaProperties nativeProperties;
    private final SystemConfigService systemConfigService;
    private final RestClient ollamaEmbedRestClient;

    @Value("${app.inference.model}")
    private String modelName;

    @Value("${app.inference.embed-model:nomic-embed-text}")
    private String embedModel;

    @Value("${app.inference.max-tokens:4096}")
    private int maxTokens;

    @Value("${app.inference.temperature:0.7}")
    private double temperature;

    private volatile NativeLlamaModelBridge model;
    private volatile NativeLlamaStatus status = NativeLlamaStatus.UNLOADED;
    private volatile String activeModelFilename;
    private volatile String errorMessage;

    /**
     * Constructs the native llama inference service.
     *
     * @param inferenceProperties   shared inference configuration (models dir, etc.)
     * @param nativeProperties      native provider-specific properties
     * @param systemConfigService   system config service for active model lookup
     * @param ollamaEmbedRestClient blocking client pointed at Ollama for embeddings
     */
    public NativeLlamaInferenceService(InferenceProperties inferenceProperties,
                                        NativeLlamaProperties nativeProperties,
                                        SystemConfigService systemConfigService,
                                        @Qualifier("ollamaEmbedRestClient") RestClient ollamaEmbedRestClient) {
        this.inferenceProperties = inferenceProperties;
        this.nativeProperties = nativeProperties;
        this.systemConfigService = systemConfigService;
        this.ollamaEmbedRestClient = ollamaEmbedRestClient;
    }

    /**
     * Auto-loads the active model on application startup if one is configured
     * in the system config.
     */
    @EventListener(ApplicationReadyEvent.class)
    public void onApplicationReady() {
        String filename = systemConfigService.getConfig().getActiveModelFilename();
        if (filename != null && !filename.isBlank()) {
            log.info("Auto-loading model on startup: {}", filename);
            try {
                loadModel(filename);
            } catch (Exception e) {
                log.error("Failed to auto-load model on startup: {}", e.getMessage());
            }
        } else {
            log.info("No active model configured — skipping auto-load");
        }
    }

    /** {@inheritDoc} */
    @Override
    public String chat(List<OllamaMessage> messages, UUID userId) {
        log.debug("Native llama sync chat for user {}", userId);

        if (model == null) {
            throw new RuntimeException("No model loaded for native inference");
        }

        String prompt = formatChatML(messages);
        InferenceParameters params = new InferenceParameters(prompt)
                .setTemperature((float) temperature)
                .setNPredict(maxTokens)
                .setStopStrings("<|im_end|>");

        String response = model.complete(params);
        return stripThinkTags(response);
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
        log.debug("Native llama streaming chat with thinking for user {}", userId);

        if (model == null) {
            return Flux.error(new RuntimeException("No model loaded for native inference"));
        }

        String prompt = formatChatML(messages);
        InferenceParameters params = new InferenceParameters(prompt)
                .setTemperature((float) temperature)
                .setNPredict(maxTokens)
                .setStopStrings("<|im_end|>");

        AtomicReference<ThinkState> state = new AtomicReference<>(ThinkState.OUTSIDE_THINK);
        StringBuilder tagBuffer = new StringBuilder();
        AtomicLong startNanos = new AtomicLong(System.nanoTime());
        AtomicLong endNanos = new AtomicLong(0);
        AtomicInteger tokenCount = new AtomicInteger(0);

        Sinks.Many<InferenceChunk> sink = Sinks.many().unicast().onBackpressureBuffer();

        Schedulers.boundedElastic().schedule(() -> {
            try {
                for (LlamaOutput output : model.generate(params)) {
                    String token = output.toString();
                    if (token != null && !token.isEmpty()) {
                        tokenCount.incrementAndGet();
                        processContentToken(token, state, tagBuffer, sink);
                    }
                }

                // Flush remaining buffer
                flushBuffer(tagBuffer, state.get(), sink);

                endNanos.set(System.nanoTime());
                double inferenceTime = (endNanos.get() - startNanos.get()) / 1e9;
                int tokens = tokenCount.get();
                double tokPerSec = inferenceTime > 0 ? tokens / inferenceTime : 0;

                sink.tryEmitNext(new InferenceChunk(
                        ChunkType.DONE, null,
                        new InferenceMetadata(tokens, tokPerSec, inferenceTime, "stop")));
                sink.tryEmitComplete();
            } catch (Exception e) {
                log.error("Native inference streaming error", e);
                sink.tryEmitError(e);
            }
        });

        return sink.asFlux();
    }

    /** {@inheritDoc} */
    @Override
    public float[] embed(String text) {
        if (nativeProperties.isEnableEmbedding() && model != null) {
            log.debug("Embedding via native llama model");
            return model.embed(text);
        }

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
        return model != null && status == NativeLlamaStatus.READY;
    }

    /** {@inheritDoc} */
    @Override
    public List<InferenceModelInfo> listModels() {
        if (activeModelFilename == null) {
            return List.of();
        }
        return List.of(new InferenceModelInfo(
                activeModelFilename,
                activeModelFilename,
                null,
                "gguf",
                null));
    }

    /** {@inheritDoc} */
    @Override
    public InferenceModelInfo getActiveModel() {
        if (activeModelFilename != null) {
            return new InferenceModelInfo(activeModelFilename, activeModelFilename, null, "gguf", null);
        }
        return new InferenceModelInfo(modelName, modelName, null, "gguf", null);
    }

    /**
     * Loads a GGUF model file from the configured models directory.
     *
     * @param filename the GGUF model filename to load
     * @throws IllegalArgumentException if the model file is not found
     */
    public void loadModel(String filename) {
        Path modelPath = resolveModelPath(filename);
        if (modelPath == null) {
            throw new IllegalArgumentException("Model file not found in models directory: " + filename);
        }

        unloadModel();

        status = NativeLlamaStatus.LOADING;
        errorMessage = null;

        try {
            log.info("Loading native model: {}", modelPath);

            ModelParameters modelParams = new ModelParameters()
                    .setModel(modelPath.toString())
                    .setCtxSize(nativeProperties.getContextSize())
                    .setGpuLayers(nativeProperties.getGpuLayers())
                    .setThreads(nativeProperties.getThreads());

            LlamaModel rawModel = new LlamaModel(modelParams);
            model = new NativeLlamaModelBridge() {
                @Override public String complete(InferenceParameters p) { return rawModel.complete(p); }
                @Override public Iterable<LlamaOutput> generate(InferenceParameters p) { return rawModel.generate(p); }
                @Override public float[] embed(String text) { return rawModel.embed(text); }
                @Override public void close() { rawModel.close(); }
            };
            activeModelFilename = filename;
            status = NativeLlamaStatus.READY;

            systemConfigService.setActiveModelFilename(filename);
            log.info("Native model loaded successfully: {}", filename);
        } catch (Exception e) {
            errorMessage = "Failed to load model: " + e.getMessage();
            log.error(errorMessage, e);
            status = NativeLlamaStatus.ERROR;
            model = null;
            activeModelFilename = null;
        }
    }

    /**
     * Unloads the currently loaded model and frees native memory.
     */
    public void unloadModel() {
        if (model != null) {
            try {
                log.info("Unloading native model: {}", activeModelFilename);
                model.close();
            } catch (Exception e) {
                log.warn("Error closing native model: {}", e.getMessage());
            }
            model = null;
            activeModelFilename = null;
            status = NativeLlamaStatus.UNLOADED;
        }
    }

    /**
     * Returns the current status of the native inference engine.
     *
     * @return status DTO with engine state, active model, and memory info
     */
    public NativeLlamaStatusDto getStatus() {
        Runtime runtime = Runtime.getRuntime();
        long usedMb = (runtime.totalMemory() - runtime.freeMemory()) / (1024 * 1024);

        return new NativeLlamaStatusDto(
                status,
                activeModelFilename,
                errorMessage,
                usedMb);
    }

    /**
     * Releases the native model on application shutdown.
     */
    @Override
    public void destroy() {
        unloadModel();
    }

    // ──────────────────────────────────────────────────────────────
    //  ChatML prompt formatting
    // ──────────────────────────────────────────────────────────────

    /**
     * Converts a list of chat messages to ChatML format.
     *
     * @param messages the conversation messages
     * @return the formatted ChatML prompt string
     */
    String formatChatML(List<OllamaMessage> messages) {
        StringBuilder sb = new StringBuilder();
        for (OllamaMessage msg : messages) {
            sb.append("<|im_start|>").append(msg.role()).append('\n');
            sb.append(msg.content()).append('\n');
            sb.append("<|im_end|>\n");
        }
        sb.append("<|im_start|>assistant\n");
        return sb.toString();
    }

    // ──────────────────────────────────────────────────────────────
    //  Think-tag state machine
    // ──────────────────────────────────────────────────────────────

    /**
     * Think-tag state machine. Accumulates characters in a buffer to detect
     * {@code <think>} and {@code </think>} tag boundaries even when they are
     * split across multiple output tokens.
     */
    void processContentToken(
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
                    int partialLen = partialTagMatchLength(buf, AppConstants.THINK_TAG_OPEN);
                    if (partialLen > 0 && partialLen == buf.length()) {
                        return;
                    }
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

                if (thinkIdx > 0) {
                    sink.tryEmitNext(new InferenceChunk(ChunkType.CONTENT,
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

                if (closeIdx > 0) {
                    sink.tryEmitNext(new InferenceChunk(ChunkType.THINKING,
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
    }

    /**
     * Flushes any remaining content in the tag buffer.
     */
    void flushBuffer(StringBuilder tagBuffer, ThinkState state,
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
     *
     * @param text the text to check
     * @param tag  the tag to match against
     * @return the length of the partial match, or 0 if none
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
    String stripThinkTags(String content) {
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

    // ──────────────────────────────────────────────────────────────
    //  Private helpers
    // ──────────────────────────────────────────────────────────────

    private Path resolveModelPath(String filename) {
        if (filename == null || filename.isBlank()) {
            return null;
        }

        Path modelsDir = Path.of(inferenceProperties.getModelsDir());
        if (!Files.exists(modelsDir)) {
            return null;
        }

        try (var stream = Files.walk(modelsDir, 3)) {
            return stream
                    .filter(Files::isRegularFile)
                    .filter(p -> p.getFileName().toString().equals(filename))
                    .findFirst()
                    .orElse(null);
        } catch (IOException e) {
            log.error("Failed to search models directory: {}", e.getMessage());
            return null;
        }
    }
}
