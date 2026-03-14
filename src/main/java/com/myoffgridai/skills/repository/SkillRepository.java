package com.myoffgridai.skills.repository;

import com.myoffgridai.skills.model.Skill;
import com.myoffgridai.skills.model.SkillCategory;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface SkillRepository extends JpaRepository<Skill, UUID> {

    List<Skill> findByIsEnabledTrue();

    List<Skill> findByIsBuiltInTrue();

    List<Skill> findByCategory(SkillCategory category);

    Optional<Skill> findByName(String name);

    List<Skill> findByIsEnabledTrueOrderByDisplayNameAsc();
}
