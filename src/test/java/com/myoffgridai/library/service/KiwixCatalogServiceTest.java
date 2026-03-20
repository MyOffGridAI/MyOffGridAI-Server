package com.myoffgridai.library.service;

import com.myoffgridai.library.config.KiwixProperties;
import com.myoffgridai.library.dto.KiwixCatalogSearchResultDto;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

import java.util.function.Function;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/**
 * Unit tests for {@link KiwixCatalogService}.
 */
@ExtendWith(MockitoExtension.class)
class KiwixCatalogServiceTest {

    @Mock private WebClient webClient;
    @Mock private KiwixProperties kiwixProperties;

    private KiwixCatalogService service;

    private static final String ATOM_FEED = """
            <?xml version="1.0" encoding="UTF-8"?>
            <feed xmlns="http://www.w3.org/2005/Atom"
                  xmlns:opensearch="http://a9.com/-/spec/opensearch/1.1/">
              <opensearch:totalResults>2</opensearch:totalResults>
              <entry>
                <id>urn:uuid:abc-123</id>
                <title>Wikipedia (English)</title>
                <summary>English Wikipedia</summary>
                <language>eng</language>
                <name>wikipedia_en</name>
                <category term="wikipedia"/>
                <tags>wikipedia;english</tags>
                <articleCount>6000000</articleCount>
                <mediaCount>500000</mediaCount>
                <link type="application/x-zim" href="/content/wikipedia_en.zim.meta4" length="95000000000"/>
              </entry>
              <entry>
                <id>urn:uuid:def-456</id>
                <title>Wiktionary (French)</title>
                <summary>French Wiktionary</summary>
                <language>fra</language>
                <name>wiktionary_fr</name>
                <category term="wiktionary"/>
                <tags>wiktionary;french</tags>
                <articleCount>200000</articleCount>
                <mediaCount>10000</mediaCount>
                <link type="application/x-zim" href="/content/wiktionary_fr.zim.meta4" length="500000000"/>
              </entry>
            </feed>
            """;

    @BeforeEach
    void setUp() {
        lenient().when(kiwixProperties.getCatalogBaseUrl()).thenReturn("https://library.kiwix.org");
        service = new KiwixCatalogService(webClient, kiwixProperties);
    }

    @Test
    @SuppressWarnings("unchecked")
    void browse_parsesAtomFeedCorrectly() {
        stubWebClientGet(Mono.just(ATOM_FEED));

        KiwixCatalogSearchResultDto result = service.browse(null, null, 20, 0);

        assertThat(result.totalCount()).isEqualTo(2);
        assertThat(result.entries()).hasSize(2);
        assertThat(result.entries().get(0).title()).isEqualTo("Wikipedia (English)");
        assertThat(result.entries().get(0).language()).isEqualTo("eng");
        assertThat(result.entries().get(0).articleCount()).isEqualTo(6000000);
        assertThat(result.entries().get(0).sizeBytes()).isEqualTo(95000000000L);
        assertThat(result.entries().get(0).downloadUrl()).isEqualTo("/content/wikipedia_en.zim");
        assertThat(result.entries().get(0).illustrationUrl()).contains("abc-123");
        assertThat(result.entries().get(1).title()).isEqualTo("Wiktionary (French)");
    }

    @Test
    @SuppressWarnings("unchecked")
    void browse_cachesResults() {
        stubWebClientGet(Mono.just(ATOM_FEED));

        service.browse("eng", null, 20, 0);
        service.browse("eng", null, 20, 0);

        // Only one API call (second served from cache)
        verify(webClient, times(1)).get();
    }

    @Test
    @SuppressWarnings("unchecked")
    void browse_throwsWhenNoCacheAndServerFails() {
        stubWebClientGet(Mono.error(new RuntimeException("Connection refused")));

        assertThatThrownBy(() -> service.browse("nocache", "nocategory", 10, 0))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("Kiwix catalog browse unavailable");
    }

    @Test
    @SuppressWarnings("unchecked")
    void search_parsesResultsCorrectly() {
        stubWebClientGet(Mono.just(ATOM_FEED));

        KiwixCatalogSearchResultDto result = service.search("wikipedia", null, 20);

        assertThat(result.totalCount()).isEqualTo(2);
        assertThat(result.entries()).hasSize(2);
    }

    @Test
    @SuppressWarnings("unchecked")
    void search_throwsOnServerError() {
        stubWebClientGet(Mono.error(new RuntimeException("Connection refused")));

        assertThatThrownBy(() -> service.search("fail", null, 20))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("Kiwix catalog search unavailable");
    }

    @Test
    @SuppressWarnings("unchecked")
    void browse_handlesEmptyFeed() {
        String emptyFeed = """
                <?xml version="1.0" encoding="UTF-8"?>
                <feed xmlns="http://www.w3.org/2005/Atom"
                      xmlns:opensearch="http://a9.com/-/spec/opensearch/1.1/">
                  <opensearch:totalResults>0</opensearch:totalResults>
                </feed>
                """;
        stubWebClientGet(Mono.just(emptyFeed));

        KiwixCatalogSearchResultDto result = service.browse("empty", null, 20, 0);

        assertThat(result.totalCount()).isZero();
        assertThat(result.entries()).isEmpty();
    }

    @Test
    @SuppressWarnings("unchecked")
    void browse_handlesEmptyXmlResponse() {
        stubWebClientGet(Mono.just(""));

        KiwixCatalogSearchResultDto result = service.browse("blank", null, 20, 0);
        assertThat(result.totalCount()).isZero();
    }

    @Test
    @SuppressWarnings("unchecked")
    void browse_stripsMetaSuffix_fromDownloadUrl() {
        String feedWithMeta4 = """
                <?xml version="1.0" encoding="UTF-8"?>
                <feed xmlns="http://www.w3.org/2005/Atom">
                  <entry>
                    <id>urn:uuid:test-id</id>
                    <title>Test ZIM</title>
                    <link type="application/x-zim" href="https://download.kiwix.org/test.zim.meta4" length="1024"/>
                  </entry>
                </feed>
                """;
        stubWebClientGet(Mono.just(feedWithMeta4));

        KiwixCatalogSearchResultDto result = service.browse("meta4test", null, 10, 0);

        assertThat(result.entries()).hasSize(1);
        assertThat(result.entries().get(0).downloadUrl())
                .isEqualTo("https://download.kiwix.org/test.zim");
    }

    @SuppressWarnings("unchecked")
    private void stubWebClientGet(Mono<String> response) {
        WebClient.RequestHeadersUriSpec requestSpec = mock(WebClient.RequestHeadersUriSpec.class);
        WebClient.RequestHeadersSpec headersSpec = mock(WebClient.RequestHeadersSpec.class);
        WebClient.ResponseSpec responseSpec = mock(WebClient.ResponseSpec.class);

        when(webClient.get()).thenReturn(requestSpec);
        when(requestSpec.uri(any(Function.class))).thenReturn(headersSpec);
        when(headersSpec.retrieve()).thenReturn(responseSpec);
        when(responseSpec.bodyToMono(String.class)).thenReturn(response);
    }
}
