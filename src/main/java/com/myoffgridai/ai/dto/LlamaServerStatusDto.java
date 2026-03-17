package com.myoffgridai.ai.dto;

import com.myoffgridai.ai.service.LlamaServerStatus;

import java.util.List;

/**
 * Data transfer object representing the current state of the llama-server process.
 *
 * @param status           the current process lifecycle status
 * @param activeModel      the filename of the currently loaded model, or null
 * @param port             the HTTP port llama-server is bound to
 * @param recentLogLines   the most recent log output lines from the process
 * @param errorMessage     a descriptive error message if status is ERROR, or null
 */
public record LlamaServerStatusDto(
        LlamaServerStatus status,
        String activeModel,
        int port,
        List<String> recentLogLines,
        String errorMessage
) {}
