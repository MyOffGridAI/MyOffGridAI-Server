package com.myoffgridai.skills.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.myoffgridai.common.exception.EntityNotFoundException;
import com.myoffgridai.common.exception.SkillDisabledException;
import com.myoffgridai.skills.model.*;
import com.myoffgridai.skills.repository.SkillExecutionRepository;
import com.myoffgridai.skills.repository.SkillRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class SkillExecutorServiceTest {

    @Mock private SkillRepository skillRepository;
    @Mock private SkillExecutionRepository executionRepository;
    @Mock private BuiltInSkill mockSkill;

    private SkillExecutorService executorService;
    private UUID userId;
    private Skill skill;

    @BeforeEach
    void setUp() {
        userId = UUID.randomUUID();

        when(mockSkill.getSkillName()).thenReturn("test-skill");

        executorService = new SkillExecutorService(
                skillRepository, executionRepository,
                List.of(mockSkill), new ObjectMapper());

        skill = new Skill();
        skill.setId(UUID.randomUUID());
        skill.setName("test-skill");
        skill.setDisplayName("Test Skill");
        skill.setDescription("A test skill");
        skill.setVersion("1.0.0");
        skill.setAuthor("test");
        skill.setCategory(SkillCategory.HOMESTEAD);
        skill.setIsEnabled(true);
        skill.setIsBuiltIn(true);
    }

    @Test
    void execute_validSkill_returnsCompletedExecution() {
        when(skillRepository.findById(skill.getId())).thenReturn(Optional.of(skill));
        when(mockSkill.execute(eq(userId), any())).thenReturn(Map.of("result", "ok"));
        when(executionRepository.save(any(SkillExecution.class))).thenAnswer(i -> i.getArgument(0));

        SkillExecution result = executorService.execute(skill.getId(), userId, Map.of());

        assertEquals(ExecutionStatus.COMPLETED, result.getStatus());
        assertNotNull(result.getOutputResult());
        assertNotNull(result.getCompletedAt());
        verify(executionRepository, times(2)).save(any(SkillExecution.class));
    }

    @Test
    void execute_skillNotFound_throwsEntityNotFound() {
        UUID unknownId = UUID.randomUUID();
        when(skillRepository.findById(unknownId)).thenReturn(Optional.empty());

        assertThrows(EntityNotFoundException.class,
                () -> executorService.execute(unknownId, userId, Map.of()));
    }

    @Test
    void execute_disabledSkill_throwsSkillDisabled() {
        skill.setIsEnabled(false);
        when(skillRepository.findById(skill.getId())).thenReturn(Optional.of(skill));

        assertThrows(SkillDisabledException.class,
                () -> executorService.execute(skill.getId(), userId, Map.of()));
    }

    @Test
    void execute_skillThrows_returnsFailedExecution() {
        when(skillRepository.findById(skill.getId())).thenReturn(Optional.of(skill));
        when(mockSkill.execute(eq(userId), any())).thenThrow(new RuntimeException("boom"));
        when(executionRepository.save(any(SkillExecution.class))).thenAnswer(i -> i.getArgument(0));

        SkillExecution result = executorService.execute(skill.getId(), userId, Map.of());

        assertEquals(ExecutionStatus.FAILED, result.getStatus());
        assertEquals("boom", result.getErrorMessage());
        assertNotNull(result.getCompletedAt());
    }

    @Test
    void executeByName_validName_returnsCompletedExecution() {
        when(skillRepository.findByName("test-skill")).thenReturn(Optional.of(skill));
        when(mockSkill.execute(eq(userId), any())).thenReturn(Map.of("data", "value"));
        when(executionRepository.save(any(SkillExecution.class))).thenAnswer(i -> i.getArgument(0));

        SkillExecution result = executorService.executeByName("test-skill", userId, Map.of());

        assertEquals(ExecutionStatus.COMPLETED, result.getStatus());
    }

    @Test
    void executeByName_unknownName_throwsEntityNotFound() {
        when(skillRepository.findByName("unknown")).thenReturn(Optional.empty());

        assertThrows(EntityNotFoundException.class,
                () -> executorService.executeByName("unknown", userId, Map.of()));
    }

    @Test
    void executeByName_disabledSkill_throwsSkillDisabled() {
        skill.setIsEnabled(false);
        when(skillRepository.findByName("test-skill")).thenReturn(Optional.of(skill));

        assertThrows(SkillDisabledException.class,
                () -> executorService.executeByName("test-skill", userId, Map.of()));
    }

    @Test
    void execute_noImplementation_returnsFailedExecution() {
        Skill unregistered = new Skill();
        unregistered.setId(UUID.randomUUID());
        unregistered.setName("no-impl");
        unregistered.setDisplayName("No Impl");
        unregistered.setDescription("No implementation");
        unregistered.setVersion("1.0.0");
        unregistered.setAuthor("test");
        unregistered.setCategory(SkillCategory.CUSTOM);
        unregistered.setIsEnabled(true);

        when(skillRepository.findById(unregistered.getId())).thenReturn(Optional.of(unregistered));
        when(executionRepository.save(any(SkillExecution.class))).thenAnswer(i -> i.getArgument(0));

        SkillExecution result = executorService.execute(unregistered.getId(), userId, Map.of());

        assertEquals(ExecutionStatus.FAILED, result.getStatus());
        assertTrue(result.getErrorMessage().contains("No implementation"));
    }

    @Test
    void execute_recordsDurationMs() {
        when(skillRepository.findById(skill.getId())).thenReturn(Optional.of(skill));
        when(mockSkill.execute(eq(userId), any())).thenReturn(Map.of());
        when(executionRepository.save(any(SkillExecution.class))).thenAnswer(i -> i.getArgument(0));

        SkillExecution result = executorService.execute(skill.getId(), userId, Map.of());

        assertNotNull(result.getDurationMs());
        assertTrue(result.getDurationMs() >= 0);
    }

    @Test
    void execute_nullParams_passesEmptyMap() {
        when(skillRepository.findById(skill.getId())).thenReturn(Optional.of(skill));
        when(mockSkill.execute(eq(userId), any())).thenReturn(Map.of());
        when(executionRepository.save(any(SkillExecution.class))).thenAnswer(i -> i.getArgument(0));

        SkillExecution result = executorService.execute(skill.getId(), userId, null);

        assertEquals(ExecutionStatus.COMPLETED, result.getStatus());
        verify(mockSkill).execute(eq(userId), eq(Map.of()));
    }
}
