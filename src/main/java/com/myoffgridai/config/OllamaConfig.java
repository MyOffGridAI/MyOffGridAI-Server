package com.myoffgridai.config;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.web.client.RestClient;
import org.springframework.web.reactive.function.client.ExchangeStrategies;
import org.springframework.web.reactive.function.client.WebClient;

/**
 * Configuration for Ollama LLM service HTTP clients.
 *
 * <p>Provides a blocking {@link RestClient} for synchronous calls (chat, embed, health)
 * and a reactive {@link WebClient} for Server-Sent Events streaming responses.</p>
 */
@Configuration
public class OllamaConfig {

    private static final Logger log = LoggerFactory.getLogger(OllamaConfig.class);

    /**
     * Creates a blocking {@link RestClient} configured for Ollama API calls.
     *
     * <p>Uses the base URL, connect timeout, and read timeout from {@link AppConstants}.
     * Sets default Content-Type to application/json.</p>
     *
     * @return the configured Ollama REST client
     */
    @Bean("ollamaRestClient")
    public RestClient ollamaRestClient() {
        log.info("Configuring Ollama RestClient with base URL: {}", AppConstants.OLLAMA_BASE_URL);

        SimpleClientHttpRequestFactory factory = new SimpleClientHttpRequestFactory();
        factory.setConnectTimeout(AppConstants.OLLAMA_CONNECT_TIMEOUT_SECONDS * 1000);
        factory.setReadTimeout(AppConstants.OLLAMA_READ_TIMEOUT_SECONDS * 1000);

        return RestClient.builder()
                .baseUrl(AppConstants.OLLAMA_BASE_URL)
                .requestFactory(factory)
                .defaultHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                .build();
    }

    /**
     * Creates a reactive {@link WebClient} for streaming SSE responses from Ollama.
     *
     * <p>Configured with the same base URL and a 10MB in-memory buffer to handle
     * large streamed responses.</p>
     *
     * @return the configured Ollama WebClient for streaming
     */
    @Bean("ollamaWebClient")
    public WebClient ollamaWebClient() {
        log.info("Configuring Ollama WebClient for streaming with base URL: {}", AppConstants.OLLAMA_BASE_URL);

        ExchangeStrategies strategies = ExchangeStrategies.builder()
                .codecs(codecs -> codecs.defaultCodecs()
                        .maxInMemorySize(10 * 1024 * 1024))
                .build();

        return WebClient.builder()
                .baseUrl(AppConstants.OLLAMA_BASE_URL)
                .defaultHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                .exchangeStrategies(strategies)
                .build();
    }
}
