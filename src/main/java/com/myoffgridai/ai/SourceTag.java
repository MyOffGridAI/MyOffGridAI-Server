package com.myoffgridai.ai;

/**
 * Indicates the origin of an assistant message's final content.
 *
 * <p>Used to distinguish between responses generated entirely by the local
 * model and those that were refined or replaced by a cloud frontier model
 * after the AI judge determined the local response was insufficient.</p>
 */
public enum SourceTag {

    /** Response generated entirely by the local model. */
    LOCAL,

    /** Response generated locally then refined/augmented by a cloud frontier model. */
    ENHANCED
}
