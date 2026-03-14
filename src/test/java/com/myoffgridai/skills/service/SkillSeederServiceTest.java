package com.myoffgridai.skills.service;

import com.myoffgridai.config.AppConstants;
import com.myoffgridai.skills.model.Skill;
import com.myoffgridai.skills.model.SkillCategory;
import com.myoffgridai.skills.repository.SkillRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class SkillSeederServiceTest {

    @Mock private SkillRepository skillRepository;

    private SkillSeederService seederService;

    @BeforeEach
    void setUp() {
        seederService = new SkillSeederService(skillRepository);
    }

    @Test
    void seedBuiltInSkills_seedsAllSixSkills_whenNoneExist() {
        when(skillRepository.findByName(anyString())).thenReturn(Optional.empty());
        when(skillRepository.save(any(Skill.class))).thenAnswer(i -> i.getArgument(0));

        seederService.seedBuiltInSkills();

        verify(skillRepository, times(6)).save(any(Skill.class));
    }

    @Test
    void seedBuiltInSkills_skipsExistingSkills() {
        Skill existing = new Skill();
        existing.setName(AppConstants.SKILL_WEATHER_QUERY);

        when(skillRepository.findByName(AppConstants.SKILL_WEATHER_QUERY))
                .thenReturn(Optional.of(existing));
        when(skillRepository.findByName(argThat(name ->
                !AppConstants.SKILL_WEATHER_QUERY.equals(name))))
                .thenReturn(Optional.empty());
        when(skillRepository.save(any(Skill.class))).thenAnswer(i -> i.getArgument(0));

        seederService.seedBuiltInSkills();

        verify(skillRepository, times(5)).save(any(Skill.class));
    }

    @Test
    void seedBuiltInSkills_allExist_savesNone() {
        when(skillRepository.findByName(anyString())).thenReturn(Optional.of(new Skill()));

        seederService.seedBuiltInSkills();

        verify(skillRepository, never()).save(any(Skill.class));
    }

    @Test
    void seedBuiltInSkills_setsCorrectMetadata() {
        when(skillRepository.findByName(anyString())).thenReturn(Optional.empty());
        ArgumentCaptor<Skill> captor = ArgumentCaptor.forClass(Skill.class);
        when(skillRepository.save(captor.capture())).thenAnswer(i -> i.getArgument(0));

        seederService.seedBuiltInSkills();

        Skill weatherSkill = captor.getAllValues().stream()
                .filter(s -> AppConstants.SKILL_WEATHER_QUERY.equals(s.getName()))
                .findFirst()
                .orElseThrow();

        assertEquals("Weather Query", weatherSkill.getDisplayName());
        assertEquals("1.0.0", weatherSkill.getVersion());
        assertEquals("MyOffGridAI", weatherSkill.getAuthor());
        assertEquals(SkillCategory.WEATHER, weatherSkill.getCategory());
        assertTrue(weatherSkill.getIsEnabled());
        assertTrue(weatherSkill.getIsBuiltIn());
        assertNotNull(weatherSkill.getParametersSchema());
    }

    @Test
    void seedBuiltInSkills_allSkillNamesPresent() {
        when(skillRepository.findByName(anyString())).thenReturn(Optional.empty());
        ArgumentCaptor<Skill> captor = ArgumentCaptor.forClass(Skill.class);
        when(skillRepository.save(captor.capture())).thenAnswer(i -> i.getArgument(0));

        seederService.seedBuiltInSkills();

        var names = captor.getAllValues().stream().map(Skill::getName).toList();
        assertTrue(names.contains(AppConstants.SKILL_WEATHER_QUERY));
        assertTrue(names.contains(AppConstants.SKILL_INVENTORY_TRACKER));
        assertTrue(names.contains(AppConstants.SKILL_RECIPE_GENERATOR));
        assertTrue(names.contains(AppConstants.SKILL_TASK_PLANNER));
        assertTrue(names.contains(AppConstants.SKILL_DOCUMENT_SUMMARIZER));
        assertTrue(names.contains(AppConstants.SKILL_RESOURCE_CALCULATOR));
    }
}
