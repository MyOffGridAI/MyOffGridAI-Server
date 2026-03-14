package com.myoffgridai.auth.model;

/**
 * Security roles available in the MyOffGridAI system.
 *
 * <p>Roles follow the Spring Security {@code ROLE_} prefix convention
 * and are ordered from highest privilege to lowest.</p>
 */
public enum Role {

    /** Full system access, created at first boot */
    ROLE_OWNER,

    /** All features, user management */
    ROLE_ADMIN,

    /** Full AI features, own data only */
    ROLE_MEMBER,

    /** Read-only access */
    ROLE_VIEWER,

    /** Safe mode, filtered responses, no vault access */
    ROLE_CHILD
}
