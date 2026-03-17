package com.myoffgridai.ai.dto;

/**
 * Data transfer object representing the current state of the llama-server
 * child process and its loaded model.
 *
 * @param status          the current process lifecycle status (STOPPED, STARTING, RUNNING, ERROR)
 * @param activeModelPath the absolute path to the currently loaded model file, or null
 * @param port            the HTTP port llama-server is listening on
 * @param modelsDir       the configured models directory
 * @param errorMessage    a descriptive error message if status is ERROR, or null
 * @param modelLoaded     true if a model is loaded and the server is healthy
 */
public record LlamaServerStatusDto(
        String status,
        String activeModelPath,
        int port,
        String modelsDir,
        String errorMessage,
        boolean modelLoaded
) {}
