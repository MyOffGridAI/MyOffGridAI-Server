package com.myoffgridai.skills.dto;

import com.myoffgridai.skills.model.ExecutionStatus;
import com.myoffgridai.skills.model.SkillExecution;

import java.time.Instant;
import java.util.UUID;

/**
 * Data transfer object for skill execution history.
 *
 * @param id           the execution ID
 * @param skillId      the skill ID
 * @param skillName    the skill name
 * @param userId       the executing user's ID
 * @param status       the execution status
 * @param inputParams  the JSON input parameters
 * @param outputResult the JSON output result
 * @param errorMessage the error message if failed
 * @param startedAt    the execution start timestamp
 * @param completedAt  the execution completion timestamp
 * @param durationMs   the execution duration in milliseconds
 */
public record SkillExecutionDto(
        UUID id,
        UUID skillId,
        String skillName,
        UUID userId,
        ExecutionStatus status,
        String inputParams,
        String outputResult,
        String errorMessage,
        Instant startedAt,
        Instant completedAt,
        Long durationMs
) {

    /**
     * Converts a {@link SkillExecution} entity to a DTO.
     *
     * @param execution the skill execution entity
     * @return the skill execution DTO
     */
    public static SkillExecutionDto from(SkillExecution execution) {
        return new SkillExecutionDto(
                execution.getId(),
                execution.getSkill().getId(),
                execution.getSkill().getName(),
                execution.getUserId(),
                execution.getStatus(),
                execution.getInputParams(),
                execution.getOutputResult(),
                execution.getErrorMessage(),
                execution.getStartedAt(),
                execution.getCompletedAt(),
                execution.getDurationMs()
        );
    }
}
