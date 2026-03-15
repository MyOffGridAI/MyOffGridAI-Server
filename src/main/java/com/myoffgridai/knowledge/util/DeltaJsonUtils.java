package com.myoffgridai.knowledge.util;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.util.List;
import java.util.Map;

/**
 * Utility for converting between plain text and Quill Delta JSON format.
 *
 * <p>Quill Delta JSON is an array of operations. For plain text, each
 * operation is an {@code {"insert": "text"}} object. This utility handles
 * the conversion in both directions.</p>
 */
public final class DeltaJsonUtils {

    private static final ObjectMapper MAPPER = new ObjectMapper();

    private DeltaJsonUtils() {
        // Prevent instantiation
    }

    /**
     * Converts plain text to Quill Delta JSON format.
     *
     * @param plainText the plain text to convert
     * @return a JSON string in Delta format, e.g. {@code [{"insert":"text\n"}]}
     */
    public static String textToDeltaJson(String plainText) {
        if (plainText == null || plainText.isBlank()) {
            return "[{\"insert\":\"\\n\"}]";
        }
        String text = plainText.endsWith("\n") ? plainText : plainText + "\n";
        try {
            List<Map<String, Object>> ops = List.of(Map.of("insert", text));
            return MAPPER.writeValueAsString(ops);
        } catch (JsonProcessingException e) {
            // Fallback: manual JSON escaping
            String escaped = text.replace("\\", "\\\\")
                    .replace("\"", "\\\"")
                    .replace("\n", "\\n")
                    .replace("\r", "\\r")
                    .replace("\t", "\\t");
            return "[{\"insert\":\"" + escaped + "\"}]";
        }
    }

    /**
     * Extracts plain text from Quill Delta JSON.
     *
     * <p>Concatenates all string {@code insert} values from the delta operations.
     * Non-string inserts (e.g., embedded images) are skipped.</p>
     *
     * @param deltaJson the Quill Delta JSON string
     * @return the concatenated plain text, or an empty string if input is null/blank
     */
    public static String deltaJsonToText(String deltaJson) {
        if (deltaJson == null || deltaJson.isBlank()) {
            return "";
        }
        try {
            List<Map<String, Object>> ops = MAPPER.readValue(
                    deltaJson, new TypeReference<>() {});
            StringBuilder sb = new StringBuilder();
            for (Map<String, Object> op : ops) {
                Object insert = op.get("insert");
                if (insert instanceof String s) {
                    sb.append(s);
                }
            }
            return sb.toString().trim();
        } catch (JsonProcessingException e) {
            return deltaJson;
        }
    }
}
