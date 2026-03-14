package com.myoffgridai.common.exception;

/**
 * Thrown when attempting to execute a disabled skill.
 *
 * <p>Maps to HTTP 400 Bad Request.</p>
 */
public class SkillDisabledException extends RuntimeException {

    public SkillDisabledException(String message) {
        super(message);
    }
}
