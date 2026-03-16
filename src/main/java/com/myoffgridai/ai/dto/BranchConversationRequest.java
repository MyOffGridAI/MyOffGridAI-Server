package com.myoffgridai.ai.dto;

/**
 * Request body for branching a conversation at a specific message.
 *
 * @param title optional title for the new branched conversation
 */
public record BranchConversationRequest(
        String title
) {}
