package com.myoffgridai.skills.service;

import java.util.Map;
import java.util.UUID;

/**
 * Interface for built-in skill implementations. Each implementation
 * auto-registers in the skill registry via its {@link #getSkillName()}.
 */
public interface BuiltInSkill {

    /**
     * Returns the unique skill name matching {@code Skill.name} in the database.
     *
     * @return the skill name
     */
    String getSkillName();

    /**
     * Executes the skill with the given parameters.
     *
     * @param userId the executing user's ID
     * @param params the input parameters
     * @return the output result as a map
     */
    Map<String, Object> execute(UUID userId, Map<String, Object> params);
}
