package com.myoffgridai.ai.service;

/**
 * Represents the lifecycle state of the llama-server process.
 */
public enum LlamaServerStatus {

    /** The process is not running and no model is configured. */
    STOPPED,

    /** The process is starting up and waiting for the health check to pass. */
    STARTING,

    /** The process is running and the health check has passed. */
    RUNNING,

    /** The process is restarting (stop then start). */
    RESTARTING,

    /** The process failed to start or crashed. */
    ERROR
}
