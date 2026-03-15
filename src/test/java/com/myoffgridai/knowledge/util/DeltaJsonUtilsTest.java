package com.myoffgridai.knowledge.util;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.*;

class DeltaJsonUtilsTest {

    @Test
    void textToDeltaJson_producesValidDelta() {
        String result = DeltaJsonUtils.textToDeltaJson("Hello world");

        assertThat(result).contains("\"insert\"");
        assertThat(result).contains("Hello world");
        assertThat(result).startsWith("[");
        assertThat(result).endsWith("]");
    }

    @Test
    void textToDeltaJson_nullInput_returnsNewlineDelta() {
        String result = DeltaJsonUtils.textToDeltaJson(null);

        assertThat(result).isEqualTo("[{\"insert\":\"\\n\"}]");
    }

    @Test
    void textToDeltaJson_blankInput_returnsNewlineDelta() {
        String result = DeltaJsonUtils.textToDeltaJson("   ");

        assertThat(result).isEqualTo("[{\"insert\":\"\\n\"}]");
    }

    @Test
    void deltaJsonToText_extractsInsertValues() {
        String deltaJson = "[{\"insert\":\"Hello world\\n\"}]";

        String result = DeltaJsonUtils.deltaJsonToText(deltaJson);

        assertThat(result).isEqualTo("Hello world");
    }

    @Test
    void deltaJsonToText_multipleOps() {
        String deltaJson = "[{\"insert\":\"Hello \"},{\"insert\":\"world\\n\"}]";

        String result = DeltaJsonUtils.deltaJsonToText(deltaJson);

        assertThat(result).isEqualTo("Hello world");
    }

    @Test
    void deltaJsonToText_nullInput_returnsEmpty() {
        String result = DeltaJsonUtils.deltaJsonToText(null);

        assertThat(result).isEmpty();
    }

    @Test
    void deltaJsonToText_blankInput_returnsEmpty() {
        String result = DeltaJsonUtils.deltaJsonToText("  ");

        assertThat(result).isEmpty();
    }

    @Test
    void roundtrip_textToDeltaAndBack() {
        String original = "Multi-line\ncontent here";
        String deltaJson = DeltaJsonUtils.textToDeltaJson(original);
        String recovered = DeltaJsonUtils.deltaJsonToText(deltaJson);

        assertThat(recovered).isEqualTo(original);
    }

    @Test
    void deltaJsonToText_invalidJson_returnsFallback() {
        String invalid = "not valid json";

        String result = DeltaJsonUtils.deltaJsonToText(invalid);

        assertThat(result).isEqualTo("not valid json");
    }

    @Test
    void deltaJsonToText_skipsNonStringInserts() {
        String deltaJson = "[{\"insert\":\"text\"},{\"insert\":{\"image\":\"url\"}},{\"insert\":\" more\"}]";

        String result = DeltaJsonUtils.deltaJsonToText(deltaJson);

        assertThat(result).isEqualTo("text more");
    }
}
