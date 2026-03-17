package com.myoffgridai.config;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.web.client.RestClient;
import org.springframework.web.reactive.function.client.ExchangeStrategies;
import org.springframework.web.reactive.function.client.WebClient;

/**
 * Spring configuration for the llama-server HTTP inference provider.
 *
 * <p>Creates a blocking {@link RestClient} for synchronous API calls
 * (health, chat, embeddings, models) and a reactive {@link WebClient}
 * for SSE streaming responses. Also provides an Ollama embed client
 * for embedding fallback.</p>
 *
 * <p>Only active when {@code app.inference.provider=llama-server}.</p>
 */
@Configuration
@ConditionalOnProperty(name = "app.inference.provider", havingValue = "llama-server")
public class LlamaServerConfig {

    private static final Logger log = LoggerFactory.getLogger(LlamaServerConfig.class);

    /**
     * Blocking RestClient pointed at the llama-server for synchronous API calls.
     *
     * @param properties llama-server configuration
     * @return configured RestClient
     */
    @Bean("llamaServerRestClient")
    public RestClient llamaServerRestClient(LlamaServerProperties properties) {
        String baseUrl = "http://localhost:" + properties.getPort();
        log.info("Configuring llama-server RestClient with base URL: {}", baseUrl);

        SimpleClientHttpRequestFactory factory = new SimpleClientHttpRequestFactory();
        factory.setConnectTimeout(AppConstants.OLLAMA_CONNECT_TIMEOUT_SECONDS * 1000);
        factory.setReadTimeout(AppConstants.OLLAMA_READ_TIMEOUT_SECONDS * 1000);

        return RestClient.builder()
                .baseUrl(baseUrl)
                .defaultHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                .requestFactory(factory)
                .build();
    }

    /**
     * Reactive WebClient for streaming SSE responses from llama-server.
     *
     * @param properties llama-server configuration
     * @return configured WebClient for streaming
     */
    @Bean("llamaServerWebClient")
    public WebClient llamaServerWebClient(LlamaServerProperties properties) {
        String baseUrl = "http://localhost:" + properties.getPort();
        log.info("Configuring llama-server WebClient for streaming with base URL: {}", baseUrl);

        ExchangeStrategies strategies = ExchangeStrategies.builder()
                .codecs(codecs -> codecs.defaultCodecs()
                        .maxInMemorySize(10 * 1024 * 1024))
                .build();

        return WebClient.builder()
                .baseUrl(baseUrl)
                .defaultHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                .exchangeStrategies(strategies)
                .build();
    }

    /**
     * Blocking RestClient for Ollama embedding endpoint.
     * Embeddings may fall back to Ollama when llama-server does not support embeddings.
     *
     * @return configured RestClient pointed at Ollama
     */
    @Bean("ollamaEmbedRestClient")
    public RestClient ollamaEmbedRestClient() {
        var factory = new SimpleClientHttpRequestFactory();
        factory.setConnectTimeout(AppConstants.OLLAMA_CONNECT_TIMEOUT_SECONDS * 1000);
        factory.setReadTimeout(AppConstants.OLLAMA_READ_TIMEOUT_SECONDS * 1000);

        return RestClient.builder()
                .baseUrl(AppConstants.OLLAMA_BASE_URL)
                .defaultHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                .requestFactory(factory)
                .build();
    }
}
