package com.myoffgridai.system.dto;

/**
 * Data transfer object for file storage settings and disk usage.
 *
 * @param knowledgeStoragePath the filesystem path where knowledge files are stored
 * @param totalSpaceMb         total disk space in megabytes
 * @param usedSpaceMb          used disk space in megabytes
 * @param freeSpaceMb          free disk space in megabytes
 */
public record StorageSettingsDto(
        String knowledgeStoragePath,
        Long totalSpaceMb,
        Long usedSpaceMb,
        Long freeSpaceMb
) {}
