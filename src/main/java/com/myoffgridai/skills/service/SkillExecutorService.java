package com.myoffgridai.skills.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.myoffgridai.common.exception.EntityNotFoundException;
import com.myoffgridai.common.exception.SkillDisabledException;
import com.myoffgridai.skills.model.ExecutionStatus;
import com.myoffgridai.skills.model.Skill;
import com.myoffgridai.skills.model.SkillExecution;
import com.myoffgridai.skills.repository.SkillExecutionRepository;
import com.myoffgridai.skills.repository.SkillRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Central dispatcher for skill execution. Resolves the correct
 * {@link BuiltInSkill} implementation, executes it, and records
 * execution history in the database.
 */
@Service
public class SkillExecutorService {

    private static final Logger log = LoggerFactory.getLogger(SkillExecutorService.class);

    private final SkillRepository skillRepository;
    private final SkillExecutionRepository executionRepository;
    private final Map<String, BuiltInSkill> skillRegistry;
    private final ObjectMapper objectMapper;

    /**
     * Constructs the skill executor service. The skill registry is populated
     * automatically via Spring's {@code List<BuiltInSkill>} injection.
     *
     * @param skillRepository     the skill repository
     * @param executionRepository the skill execution repository
     * @param builtInSkills       the list of built-in skill implementations
     * @param objectMapper        the JSON object mapper
     */
    public SkillExecutorService(SkillRepository skillRepository,
                                 SkillExecutionRepository executionRepository,
                                 List<BuiltInSkill> builtInSkills,
                                 ObjectMapper objectMapper) {
        this.skillRepository = skillRepository;
        this.executionRepository = executionRepository;
        this.objectMapper = objectMapper;

        this.skillRegistry = new HashMap<>();
        for (BuiltInSkill skill : builtInSkills) {
            skillRegistry.put(skill.getSkillName(), skill);
            log.info("Registered built-in skill: {}", skill.getSkillName());
        }
    }

    /**
     * Executes a skill by its database ID.
     *
     * @param skillId the skill ID
     * @param userId  the executing user's ID
     * @param params  the input parameters
     * @return the completed skill execution record
     * @throws EntityNotFoundException  if the skill is not found
     * @throws SkillDisabledException   if the skill is disabled
     */
    @Transactional
    public SkillExecution execute(UUID skillId, UUID userId, Map<String, Object> params) {
        Skill skill = skillRepository.findById(skillId)
                .orElseThrow(() -> new EntityNotFoundException("Skill not found: " + skillId));

        if (!skill.getIsEnabled()) {
            throw new SkillDisabledException("Skill is disabled: " + skill.getDisplayName());
        }

        return executeSkill(skill, userId, params);
    }

    /**
     * Executes a skill by its unique name. Used by the AgentService tool-call dispatcher.
     *
     * @param skillName the skill name
     * @param userId    the executing user's ID
     * @param params    the input parameters
     * @return the completed skill execution record
     * @throws EntityNotFoundException if the skill is not found
     * @throws SkillDisabledException  if the skill is disabled
     */
    @Transactional
    public SkillExecution executeByName(String skillName, UUID userId, Map<String, Object> params) {
        Skill skill = skillRepository.findByName(skillName)
                .orElseThrow(() -> new EntityNotFoundException("Skill not found: " + skillName));

        if (!skill.getIsEnabled()) {
            throw new SkillDisabledException("Skill is disabled: " + skill.getDisplayName());
        }

        return executeSkill(skill, userId, params);
    }

    private SkillExecution executeSkill(Skill skill, UUID userId, Map<String, Object> params) {
        long startTime = System.currentTimeMillis();

        SkillExecution execution = new SkillExecution();
        execution.setSkill(skill);
        execution.setUserId(userId);
        execution.setStatus(ExecutionStatus.RUNNING);
        execution.setInputParams(toJson(params));
        execution = executionRepository.save(execution);

        log.info("Executing skill '{}' for user {}", skill.getName(), userId);

        try {
            BuiltInSkill builtInSkill = skillRegistry.get(skill.getName());
            if (builtInSkill == null) {
                throw new IllegalStateException(
                        "No implementation registered for skill: " + skill.getName());
            }

            Map<String, Object> result = builtInSkill.execute(userId, params != null ? params : Map.of());

            long durationMs = System.currentTimeMillis() - startTime;
            execution.setStatus(ExecutionStatus.COMPLETED);
            execution.setOutputResult(toJson(result));
            execution.setCompletedAt(Instant.now());
            execution.setDurationMs(durationMs);
            execution = executionRepository.save(execution);

            log.info("Skill '{}' completed in {}ms", skill.getName(), durationMs);
            return execution;

        } catch (Exception e) {
            long durationMs = System.currentTimeMillis() - startTime;
            execution.setStatus(ExecutionStatus.FAILED);
            execution.setErrorMessage(e.getMessage());
            execution.setCompletedAt(Instant.now());
            execution.setDurationMs(durationMs);
            execution = executionRepository.save(execution);

            log.error("Skill '{}' failed after {}ms: {}", skill.getName(), durationMs, e.getMessage());
            return execution;
        }
    }

    private String toJson(Object obj) {
        if (obj == null) return null;
        try {
            return objectMapper.writeValueAsString(obj);
        } catch (JsonProcessingException e) {
            log.warn("Failed to serialize to JSON: {}", e.getMessage());
            return obj.toString();
        }
    }
}
