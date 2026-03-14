package com.myoffgridai.skills.dto;

import jakarta.validation.constraints.NotNull;

import java.util.Map;
import java.util.UUID;

/**
 * Request body for executing a skill.
 *
 * @param skillId the ID of the skill to execute
 * @param params  the input parameters for the skill
 */
public record SkillExecuteRequest(
        @NotNull(message = "skillId is required")
        UUID skillId,

        Map<String, Object> params
) {
}
