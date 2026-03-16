package com.myoffgridai.skills.repository;

import com.myoffgridai.skills.model.Skill;
import com.myoffgridai.skills.model.SkillCategory;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Spring Data JPA repository for {@link Skill} entities.
 *
 * @author MyOffGridAI
 * @since 0.1.0
 */
public interface SkillRepository extends JpaRepository<Skill, UUID> {

    /**
     * Finds all enabled skills.
     *
     * @return list of enabled skills
     */
    List<Skill> findByIsEnabledTrue();

    /**
     * Finds all built-in skills.
     *
     * @return list of built-in skills
     */
    List<Skill> findByIsBuiltInTrue();

    /**
     * Finds all skills in the given category.
     *
     * @param category the skill category
     * @return list of skills in the category
     */
    List<Skill> findByCategory(SkillCategory category);

    /**
     * Finds a skill by its unique name.
     *
     * @param name the skill name
     * @return the skill if found
     */
    Optional<Skill> findByName(String name);

    /**
     * Finds all enabled skills, ordered by display name ascending.
     *
     * @return list of enabled skills sorted alphabetically
     */
    List<Skill> findByIsEnabledTrueOrderByDisplayNameAsc();
}
