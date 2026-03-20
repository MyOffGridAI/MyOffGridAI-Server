package com.myoffgridai.skills.dto;

import com.myoffgridai.skills.model.SkillCategory;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

/**
 * Request body for creating a new custom skill.
 *
 * @param name             the unique skill identifier
 * @param displayName      the human-readable display name
 * @param description      the skill description
 * @param category         the skill category
 * @param version          the skill version (optional, defaults to "1.0.0")
 * @param parametersSchema optional JSON schema for expected parameters
 */
public record CreateSkillRequest(
        @NotBlank(message = "Name is required")
        String name,

        @NotBlank(message = "Display name is required")
        String displayName,

        @NotBlank(message = "Description is required")
        String description,

        @NotNull(message = "Category is required")
        SkillCategory category,

        String version,
        String parametersSchema
) {
}
