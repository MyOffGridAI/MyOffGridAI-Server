package com.myoffgridai.models.service;

import com.myoffgridai.config.AppConstants;
import org.junit.jupiter.api.Test;
import org.springframework.http.client.reactive.JdkClientHttpConnector;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.util.DefaultUriBuilderFactory;

import java.net.URI;
import java.net.http.HttpClient;
import java.time.Duration;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Integration test verifying HuggingFace search works with queries containing
 * special characters (slashes, uppercase letters like 'J' and 'M').
 *
 * <p>Uses JdkClientHttpConnector to bypass a bug in Netty 4.1.129's
 * {@code HttpUtil.isEncodingSafeStartLineToken()} where {@code 1L << charValue}
 * wraps modulo 64, causing 'J' (74 % 64 = 10) to collide with '\n' (10)
 * and 'M' (77 % 64 = 13) to collide with '\r' (13) in the bitmask check.</p>
 */
class ModelCatalogServiceIntegrationTest {

    private static final String SLASH_QUERY = "Jackrong/Qwen3.5-27B-Claude-4.6-Opus-Reasoning-Distilled-GGUF";

    @Test
    void uriBuilding_withSlashInQuery_uriComponentMode_keepsLiteralSlash() {
        DefaultUriBuilderFactory factory = new DefaultUriBuilderFactory(AppConstants.HF_API_BASE);
        factory.setEncodingMode(DefaultUriBuilderFactory.EncodingMode.URI_COMPONENT);

        URI uri = factory.uriString("")
                .path("/models")
                .queryParam("search", SLASH_QUERY)
                .queryParam("sort", "downloads")
                .queryParam("direction", "-1")
                .queryParam("limit", 20)
                .queryParam("full", true)
                .queryParam("filter", "gguf")
                .build();

        assertFalse(uri.toString().contains("%2F"),
                "URI should not contain %2F — slash is valid in query per RFC 3986");
        assertTrue(uri.getRawQuery().contains("Jackrong/Qwen"),
                "Query should contain literal slash");
        assertTrue(uri.toString().startsWith("https://huggingface.co/api/models"),
                "URI should start with the HF API base + /models");
    }

    @Test
    void realHttpCall_withJdkClientHttpConnector_slashQuery_succeeds() {
        DefaultUriBuilderFactory factory = new DefaultUriBuilderFactory(AppConstants.HF_API_BASE);
        factory.setEncodingMode(DefaultUriBuilderFactory.EncodingMode.URI_COMPONENT);

        HttpClient jdkHttpClient = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(10))
                .build();

        WebClient webClient = WebClient.builder()
                .uriBuilderFactory(factory)
                .clientConnector(new JdkClientHttpConnector(jdkHttpClient))
                .build();

        String body = webClient.get()
                .uri(uriBuilder -> uriBuilder.path("/models")
                        .queryParam("search", SLASH_QUERY)
                        .queryParam("limit", 2)
                        .queryParam("filter", "gguf")
                        .build())
                .retrieve()
                .bodyToMono(String.class)
                .block();

        assertNotNull(body, "Response body should not be null");
        assertTrue(body.startsWith("["), "Response should be a JSON array");
    }
}
