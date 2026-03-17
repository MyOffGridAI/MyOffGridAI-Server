package com.myoffgridai.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.web.client.RestClient;

/**
 * Spring configuration for the native java-llama.cpp inference provider.
 *
 * <p>Creates the {@code ollamaEmbedRestClient} bean for embedding requests
 * that always go to Ollama regardless of the inference provider. The native
 * inference service itself is component-scanned via {@code @Service}.</p>
 *
 * <p>Only active when {@code app.inference.provider=native}.</p>
 */
@Configuration
@ConditionalOnProperty(name = "app.inference.provider", havingValue = "native")
public class NativeLlamaConfig {

    @Value("${app.ollama.base-url:" + AppConstants.OLLAMA_BASE_URL + "}")
    private String ollamaBaseUrl;

    /**
     * Blocking RestClient for Ollama embedding endpoint.
     * Embeddings always use Ollama's nomic-embed-text regardless of inference provider.
     *
     * @return configured RestClient pointed at Ollama
     */
    @Bean("ollamaEmbedRestClient")
    public RestClient ollamaEmbedRestClient() {
        var factory = new SimpleClientHttpRequestFactory();
        factory.setConnectTimeout(AppConstants.OLLAMA_CONNECT_TIMEOUT_SECONDS * 1000);
        factory.setReadTimeout(AppConstants.OLLAMA_READ_TIMEOUT_SECONDS * 1000);

        return RestClient.builder()
                .baseUrl(ollamaBaseUrl)
                .defaultHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                .requestFactory(factory)
                .build();
    }
}
