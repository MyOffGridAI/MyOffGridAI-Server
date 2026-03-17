package com.myoffgridai.models.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.myoffgridai.config.AppConstants;
import com.myoffgridai.models.dto.HfModelDto;
import com.myoffgridai.models.dto.HfModelFileDto;
import com.myoffgridai.models.dto.HfSearchResultDto;
import com.myoffgridai.settings.service.ExternalApiSettingsService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.client.reactive.JdkClientHttpConnector;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClientResponseException;
import org.springframework.web.util.DefaultUriBuilderFactory;

import java.net.http.HttpClient;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

/**
 * Searches and retrieves model metadata from the HuggingFace Hub API.
 *
 * <p>Uses the public HuggingFace API (no auth required for public models).
 * When a HuggingFace token is configured in {@link com.myoffgridai.settings.model.ExternalApiSettings},
 * it is included as a Bearer token for higher rate limits and gated model access.</p>
 *
 * <p>Results are filtered to GGUF and MLX formats by default, as these are
 * the formats supported by LM Studio on Apple Silicon.</p>
 */
@Service
public class ModelCatalogService {

    private static final Logger log = LoggerFactory.getLogger(ModelCatalogService.class);

    private final WebClient webClient;
    private final ExternalApiSettingsService settingsService;
    private final ObjectMapper objectMapper;
    private final QuantizationRecommendationService recommendationService;

    /**
     * Constructs the service.
     *
     * @param webClientBuilder      the WebClient builder
     * @param settingsService       the external API settings service for HuggingFace token
     * @param objectMapper          the Jackson object mapper
     * @param recommendationService the quantization recommendation service
     */
    public ModelCatalogService(WebClient.Builder webClientBuilder,
                               ExternalApiSettingsService settingsService,
                               ObjectMapper objectMapper,
                               QuantizationRecommendationService recommendationService) {
        // Use JDK HttpClient instead of Reactor Netty to avoid Netty 4.1.129 bug
        // in HttpUtil.isEncodingSafeStartLineToken() where chars >= 64 (e.g. 'J')
        // collide with control chars via 1L<<char modular wrapping, falsely rejecting
        // valid URIs containing uppercase letters like 'J' or 'M'.
        HttpClient httpClient = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(10))
                .build();
        DefaultUriBuilderFactory factory = new DefaultUriBuilderFactory(AppConstants.HF_API_BASE);
        factory.setEncodingMode(DefaultUriBuilderFactory.EncodingMode.URI_COMPONENT);
        this.webClient = webClientBuilder
                .uriBuilderFactory(factory)
                .clientConnector(new JdkClientHttpConnector(httpClient))
                .build();
        this.settingsService = settingsService;
        this.objectMapper = objectMapper;
        this.recommendationService = recommendationService;
    }

    /**
     * Searches HuggingFace for models matching the query with an optional format filter.
     *
     * @param query        the search query string
     * @param formatFilter the format filter ("gguf", "mlx", or "all")
     * @param limit        the maximum number of results
     * @return the search results
     */
    public HfSearchResultDto searchModels(String query, String formatFilter, int limit) {
        int clampedLimit = Math.min(Math.max(limit, 1), AppConstants.HF_SEARCH_MAX_LIMIT);
        log.info("Searching HuggingFace for '{}' (filter={}, limit={})", query, formatFilter, clampedLimit);

        try {
            var spec = webClient.get()
                    .uri(uriBuilder -> {
                        uriBuilder.path("/models")
                                .queryParam("search", query)
                                .queryParam("sort", "downloads")
                                .queryParam("direction", "-1")
                                .queryParam("limit", clampedLimit)
                                .queryParam("full", true);
                        if (formatFilter != null && !"all".equalsIgnoreCase(formatFilter)) {
                            uriBuilder.queryParam("filter", formatFilter.toLowerCase());
                        }
                        return uriBuilder.build();
                    });

            spec = addAuthHeader(spec);

            String body = spec.retrieve().bodyToMono(String.class).block();
            List<HfModelDto> models = parseModelList(body);
            return new HfSearchResultDto(models, models.size());
        } catch (WebClientResponseException.TooManyRequests e) {
            log.warn("HuggingFace rate limit hit: {}", e.getMessage());
            throw new HuggingFaceRateLimitException("HuggingFace rate limit exceeded. Try again in a few minutes.");
        } catch (WebClientResponseException.Forbidden e) {
            log.warn("HuggingFace forbidden: {}", e.getMessage());
            throw new HuggingFaceAccessDeniedException(
                    "Access denied. This may be a gated model requiring a HuggingFace token.");
        } catch (Exception e) {
            log.error("HuggingFace search failed: {}", e.getMessage());
            throw new RuntimeException("Failed to search HuggingFace: " + e.getMessage(), e);
        }
    }

    /**
     * Returns full model metadata including all available files/quantizations.
     *
     * @param repoId the repository ID (e.g. "author/model-name")
     * @return the model details
     */
    public HfModelDto getModelDetails(String repoId) {
        log.info("Fetching model details for '{}'", repoId);
        try {
            var spec = webClient.get()
                    .uri("/models/{repoId}", repoId);

            spec = addAuthHeader(spec);

            String body = spec.retrieve().bodyToMono(String.class).block();
            return parseModel(objectMapper.readTree(body));
        } catch (WebClientResponseException.TooManyRequests e) {
            throw new HuggingFaceRateLimitException("HuggingFace rate limit exceeded. Try again in a few minutes.");
        } catch (WebClientResponseException.Forbidden e) {
            throw new HuggingFaceAccessDeniedException(
                    "Access denied. This may be a gated model requiring a HuggingFace token.");
        } catch (Exception e) {
            log.error("Failed to get model details for '{}': {}", repoId, e.getMessage());
            throw new RuntimeException("Failed to get model details: " + e.getMessage(), e);
        }
    }

    /**
     * Returns the file list for a model repository with sizes.
     *
     * @param repoId the repository ID (e.g. "author/model-name")
     * @return the list of files
     */
    public List<HfModelFileDto> getModelFiles(String repoId) {
        log.info("Fetching files for model '{}'", repoId);
        try {
            var spec = webClient.get()
                    .uri(uriBuilder -> uriBuilder
                            .path("/models/{repoId}")
                            .queryParam("blobs", true)
                            .build(repoId));

            spec = addAuthHeader(spec);

            String body = spec.retrieve().bodyToMono(String.class).block();
            JsonNode root = objectMapper.readTree(body);
            return parseSiblings(root.get("siblings"));
        } catch (WebClientResponseException.TooManyRequests e) {
            throw new HuggingFaceRateLimitException("HuggingFace rate limit exceeded. Try again in a few minutes.");
        } catch (WebClientResponseException.Forbidden e) {
            throw new HuggingFaceAccessDeniedException(
                    "Access denied. This may be a gated model requiring a HuggingFace token.");
        } catch (Exception e) {
            log.error("Failed to get files for '{}': {}", repoId, e.getMessage());
            throw new RuntimeException("Failed to get model files: " + e.getMessage(), e);
        }
    }

    private WebClient.RequestHeadersSpec<?> addAuthHeader(WebClient.RequestHeadersSpec<?> spec) {
        Optional<String> token = settingsService.getHuggingFaceToken();
        if (token.isPresent()) {
            return spec.header("Authorization", "Bearer " + token.get());
        }
        return spec;
    }

    private List<HfModelDto> parseModelList(String json) {
        try {
            JsonNode root = objectMapper.readTree(json);
            if (!root.isArray()) return Collections.emptyList();
            List<HfModelDto> models = new ArrayList<>();
            for (JsonNode node : root) {
                models.add(parseModel(node));
            }
            return models;
        } catch (Exception e) {
            log.error("Failed to parse HuggingFace model list: {}", e.getMessage());
            return Collections.emptyList();
        }
    }

    private HfModelDto parseModel(JsonNode node) {
        String id = node.path("id").asText("");
        String[] parts = id.split("/", 2);
        String author = parts.length > 1 ? parts[0] : "";
        String modelId = parts.length > 1 ? parts[1] : id;

        List<String> tags = new ArrayList<>();
        if (node.has("tags") && node.get("tags").isArray()) {
            for (JsonNode tag : node.get("tags")) {
                tags.add(tag.asText());
            }
        }

        String pipelineTag = node.path("pipeline_tag").asText(null);
        boolean gated = node.path("gated").asBoolean(false)
                || "true".equals(node.path("gated").asText());

        Instant lastModified = null;
        String lastModifiedStr = node.path("lastModified").asText(null);
        if (lastModifiedStr != null) {
            try {
                lastModified = Instant.parse(lastModifiedStr);
            } catch (Exception ignored) {
                // Some HF timestamps may not parse
            }
        }

        List<HfModelFileDto> siblings = parseSiblings(node.get("siblings"));

        return new HfModelDto(
                id, modelId, author,
                node.path("downloads").asLong(0),
                node.path("likes").asLong(0),
                tags, pipelineTag, gated, lastModified, siblings
        );
    }

    private List<HfModelFileDto> parseSiblings(JsonNode siblingsNode) {
        if (siblingsNode == null || !siblingsNode.isArray()) return Collections.emptyList();
        List<HfModelFileDto> files = new ArrayList<>();
        for (JsonNode sib : siblingsNode) {
            String rfilename = sib.path("rfilename").asText("");
            Long size = sib.has("size") && !sib.get("size").isNull()
                    ? sib.get("size").asLong() : null;
            String blobId = sib.path("lfs").path("oid").asText(null);
            if (blobId == null) {
                blobId = sib.path("blobId").asText(null);
            }
            files.add(new HfModelFileDto(rfilename, size, blobId));
        }
        return recommendationService.enrichFiles(files);
    }

    /**
     * Thrown when the HuggingFace API returns 429 Too Many Requests.
     */
    public static class HuggingFaceRateLimitException extends RuntimeException {
        /**
         * @param message the error message
         */
        public HuggingFaceRateLimitException(String message) { super(message); }
    }

    /**
     * Thrown when the HuggingFace API returns 403 Forbidden (gated model, no token).
     */
    public static class HuggingFaceAccessDeniedException extends RuntimeException {
        /**
         * @param message the error message
         */
        public HuggingFaceAccessDeniedException(String message) { super(message); }
    }
}
