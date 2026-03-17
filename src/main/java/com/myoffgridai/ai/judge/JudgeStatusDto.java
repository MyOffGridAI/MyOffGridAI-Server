package com.myoffgridai.ai.judge;

/**
 * Status snapshot of the AI judge subsystem.
 *
 * @param enabled            whether the judge pipeline is enabled in settings
 * @param processRunning     whether the judge llama-server process is alive
 * @param judgeModelFilename the configured judge model GGUF filename
 * @param port               the HTTP port the judge process listens on
 * @param scoreThreshold     the minimum score below which cloud refinement is triggered
 */
public record JudgeStatusDto(
        boolean enabled,
        boolean processRunning,
        String judgeModelFilename,
        int port,
        double scoreThreshold
) {
}
