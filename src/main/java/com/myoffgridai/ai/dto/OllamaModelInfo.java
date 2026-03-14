package com.myoffgridai.ai.dto;

import java.time.Instant;

public record OllamaModelInfo(
        String name,
        long size,
        Instant modifiedAt
) {
}
