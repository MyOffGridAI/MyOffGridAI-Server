package com.myoffgridai.ai.service;

import com.myoffgridai.ai.dto.ChunkType;
import com.myoffgridai.ai.dto.InferenceChunk;
import com.myoffgridai.ai.dto.InferenceMetadata;
import com.myoffgridai.ai.dto.InferenceModelInfo;
import com.myoffgridai.ai.dto.OllamaChatRequest;
import com.myoffgridai.ai.dto.OllamaChatResponse;
import com.myoffgridai.ai.dto.OllamaMessage;
import com.myoffgridai.ai.dto.OllamaModelInfo;
import com.myoffgridai.config.AppConstants;
import com.myoffgridai.system.service.SystemConfigService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.atomic.AtomicReference;

/**
 * Inference provider implementation that delegates to the existing {@link OllamaService}.
 *
 * <p>This is a thin wrapper that adapts OllamaService to the {@link InferenceService}
 * interface. Activated when {@code app.inference.provider=ollama}.
 *
 * <p>Supports two modes of thinking detection:
 * <ol>
 *   <li>Ollama 0.6+ native {@code thinking} field — for models that support the
 *       {@code think: true} request parameter.</li>
 *   <li>{@code <think>}/{@code </think>} tag parsing in the {@code content} field —
 *       for GGUF models (e.g. Qwen-distilled) that embed reasoning traces inline
 *       using think tags. Uses a state machine identical to
 *       {@link LlamaServerInferenceService}.</li>
 * </ol>
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
     * <p>Supports two thinking detection modes:
     * <ol>
     *   <li><strong>Native field</strong> — if the Ollama chunk carries a non-empty
     *       {@code thinking} field (Ollama 0.6+ with {@code think: true}), the text
     *       is emitted directly as {@link ChunkType#THINKING}.</li>
     *   <li><strong>Tag parsing</strong> — if the model embeds
     *       {@code <think>}/{@code </think>} tags inside the {@code content} field
     *       (common with GGUF distilled models), a state machine strips the tags
     *       and classifies tokens accordingly.</li>
     * </ol>
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

        AtomicReference<ThinkState> thinkState = new AtomicReference<>(ThinkState.OUTSIDE_THINK);
        StringBuilder tagBuffer = new StringBuilder();
        AtomicLong startNanos = new AtomicLong(System.nanoTime());
        AtomicInteger tokenCount = new AtomicInteger(0);

        return ollamaService.chatStream(request)
                .concatMap(chunk -> {
                    if (chunk.message() == null) {
                        return Flux.<InferenceChunk>empty();
                    }

                    // Native thinking field (Ollama 0.6+ models with think support)
                    String thinking = chunk.message().thinking();
                    if (thinking != null && !thinking.isEmpty()) {
                        tokenCount.incrementAndGet();
                        return Flux.just(new InferenceChunk(ChunkType.THINKING, thinking, null));
                    }

                    // Content field → <think> tag state machine
                    String content = chunk.message().content();
                    if (content != null && !content.isEmpty()) {
                        tokenCount.incrementAndGet();
                        return processToken(content, thinkState, tagBuffer);
                    }

                    return Flux.<InferenceChunk>empty();
                })
                .concatWith(Flux.defer(() -> {
                    List<InferenceChunk> remaining = flushBuffer(tagBuffer, thinkState.get());
                    long endNanos = System.nanoTime();
                    double inferenceTime = (endNanos - startNanos.get()) / 1e9;
                    int tokens = tokenCount.get();
                    double tokPerSec = inferenceTime > 0 ? tokens / inferenceTime : 0;

                    List<InferenceChunk> result = new ArrayList<>(remaining);
                    result.add(new InferenceChunk(
                            ChunkType.DONE, null,
                            new InferenceMetadata(tokens, tokPerSec, inferenceTime, "stop")));
                    return Flux.fromIterable(result);
                }));
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

    // ── Think-tag state machine ──────────────────────────────────────────

    /**
     * States for the {@code <think>}/{@code </think>} tag parser.
     */
    private enum ThinkState {
        OUTSIDE_THINK,
        INSIDE_THINK
    }

    /**
     * Feeds a token through the think-tag state machine, buffering partial tag
     * matches and emitting typed {@link InferenceChunk} instances.
     *
     * @param token     the next token from the stream
     * @param state     current parser state (mutated)
     * @param tagBuffer running buffer for partial tag detection (mutated)
     * @return zero or more inference chunks parsed from the token
     */
    private Flux<InferenceChunk> processToken(
            String token,
            AtomicReference<ThinkState> state,
            StringBuilder tagBuffer) {

        List<InferenceChunk> chunks = new ArrayList<>();
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

    /**
     * Flushes any remaining content in the tag buffer when the stream ends.
     *
     * @param tagBuffer the tag parse buffer
     * @param state     the current parse state
     * @return a list containing a single chunk if the buffer was non-empty, else empty
     */
    private List<InferenceChunk> flushBuffer(StringBuilder tagBuffer, ThinkState state) {
        if (tagBuffer.length() > 0) {
            ChunkType type = state == ThinkState.INSIDE_THINK ? ChunkType.THINKING : ChunkType.CONTENT;
            InferenceChunk chunk = new InferenceChunk(type, tagBuffer.toString(), null);
            tagBuffer.setLength(0);
            return List.of(chunk);
        }
        return List.of();
    }

    /**
     * Checks if the end of {@code text} is a partial prefix of {@code tag}.
     *
     * @param text the buffer text to check
     * @param tag  the tag to match against (e.g. {@code "<think>"})
     * @return the length of the partial match, or 0 if none
     */
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
