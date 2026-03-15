package com.myoffgridai.events.model;

/**
 * Actions that a {@link ScheduledEvent} can perform when triggered.
 */
public enum ActionType {

    /** Send a push notification to the user. */
    PUSH_NOTIFICATION,

    /** Execute an AI prompt and deliver the response. */
    AI_PROMPT,

    /** Generate an AI summary of recent activity. */
    AI_SUMMARY
}
