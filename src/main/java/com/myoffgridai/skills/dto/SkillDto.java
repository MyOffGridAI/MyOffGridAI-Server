package com.myoffgridai.skills.dto;

import com.myoffgridai.skills.model.Skill;
import com.myoffgridai.skills.model.SkillCategory;

import java.time.Instant;
import java.util.UUID;

/**
 * Data transfer object for skill metadata.
 *
 * @param id               the skill ID
 * @param name             the unique skill name
 * @param displayName      the human-readable display name
 * @param description      the skill description
 * @param version          the skill version
 * @param author           the skill author
 * @param category         the skill category
 * @param isEnabled        whether the skill is enabled
 * @param isBuiltIn        whether the skill is built-in
 * @param parametersSchema the JSON schema for expected parameters
 * @param createdAt        the creation timestamp
 * @param updatedAt        the last update timestamp
 */
public record SkillDto(
        UUID id,
        String name,
        String displayName,
        String description,
        String version,
        String author,
        SkillCategory category,
        boolean isEnabled,
        boolean isBuiltIn,
        String parametersSchema,
        Instant createdAt,
        Instant updatedAt
) {

    /**
     * Converts a {@link Skill} entity to a DTO.
     *
     * @param skill the skill entity
     * @return the skill DTO
     */
    public static SkillDto from(Skill skill) {
        return new SkillDto(
                skill.getId(),
                skill.getName(),
                skill.getDisplayName(),
                skill.getDescription(),
                skill.getVersion(),
                skill.getAuthor(),
                skill.getCategory(),
                skill.getIsEnabled(),
                skill.getIsBuiltIn(),
                skill.getParametersSchema(),
                skill.getCreatedAt(),
                skill.getUpdatedAt()
        );
    }
}
