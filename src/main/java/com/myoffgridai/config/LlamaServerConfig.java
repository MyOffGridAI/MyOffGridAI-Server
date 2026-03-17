package com.myoffgridai.config;

import com.myoffgridai.ai.service.ProcessBuilderFactory;
import org.springframework.beans.factory.annotation.Value;
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
 * Spring configuration for llama-server HTTP clients.
 *
 * <p>Creates {@code llamaServerWebClient} (reactive, for SSE streaming) and
 * {@code llamaServerRestClient} (blocking, for sync calls and model listing).
 * Also creates {@code ollamaEmbedRestClient} for embedding requests that
 * always go to Ollama regardless of the inference provider.
 *
 * <p>Only active when {@code app.inference.provider=llama-server}.
 */
@Configuration
@ConditionalOnProperty(name = "app.inference.provider", havingValue = "llama-server")
public class LlamaServerConfig {

    @Value("${app.inference.base-url:http://localhost:1234}")
    private String llamaServerBaseUrl;

    @Value("${app.inference.timeout-seconds:120}")
    private int timeoutSeconds;

    @Value("${app.ollama.base-url:" + AppConstants.OLLAMA_BASE_URL + "}")
    private String ollamaBaseUrl;

    /**
     * Reactive WebClient for llama-server SSE streaming responses.
     *
     * @return configured WebClient pointed at llama-server
     */
    @Bean("llamaServerWebClient")
    public WebClient llamaServerWebClient() {
        return WebClient.builder()
                .baseUrl(llamaServerBaseUrl)
                .defaultHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                .exchangeStrategies(ExchangeStrategies.builder()
                        .codecs(config -> config.defaultCodecs().maxInMemorySize(10 * 1024 * 1024))
                        .build())
                .build();
    }

    /**
     * Blocking RestClient for llama-server sync requests and model listing.
     *
     * @return configured RestClient pointed at llama-server
     */
    @Bean("llamaServerRestClient")
    public RestClient llamaServerRestClient() {
        var factory = new SimpleClientHttpRequestFactory();
        factory.setConnectTimeout(AppConstants.OLLAMA_CONNECT_TIMEOUT_SECONDS * 1000);
        factory.setReadTimeout(timeoutSeconds * 1000);

        return RestClient.builder()
                .baseUrl(llamaServerBaseUrl)
                .defaultHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                .requestFactory(factory)
                .build();
    }

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

    /**
     * Factory for creating {@link ProcessBuilder} instances, enabling testability
     * of process management in {@link com.myoffgridai.ai.service.LlamaServerProcessService}.
     *
     * @return the default ProcessBuilderFactory using the system ProcessBuilder
     */
    @Bean
    public ProcessBuilderFactory processBuilderFactory() {
        return ProcessBuilder::new;
    }
}
