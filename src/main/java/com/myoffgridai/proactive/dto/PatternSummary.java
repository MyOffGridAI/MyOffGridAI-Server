package com.myoffgridai.proactive.dto;

import java.util.List;
import java.util.Map;

/**
 * Summary of a user's recent activity patterns used for insight generation.
 *
 * @param recentConversationCount  number of recent conversations
 * @param recentConversationTitles titles of recent conversations
 * @param highImportanceMemories   content of high/critical importance memories
 * @param sensorAverages           average sensor readings by type name
 * @param lowStockItems            names of inventory items below stock threshold
 * @param activeTasks              titles of active planned tasks
 * @param analysisWindowDays       the analysis window in days
 */
public record PatternSummary(
        int recentConversationCount,
        List<String> recentConversationTitles,
        List<String> highImportanceMemories,
        Map<String, Double> sensorAverages,
        List<String> lowStockItems,
        List<String> activeTasks,
        int analysisWindowDays
) {

    /**
     * Checks whether this summary contains any meaningful data for insight generation.
     *
     * @return true if there is at least some data to analyze
     */
    public boolean hasData() {
        return recentConversationCount > 0
                || !highImportanceMemories.isEmpty()
                || !sensorAverages.isEmpty()
                || !lowStockItems.isEmpty()
                || !activeTasks.isEmpty();
    }
}
