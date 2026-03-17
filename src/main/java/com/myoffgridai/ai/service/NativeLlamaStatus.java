package com.myoffgridai.ai.service;

/**
 * Represents the lifecycle state of the native java-llama.cpp inference engine.
 */
public enum NativeLlamaStatus {

    /** No model is loaded in memory. */
    UNLOADED,

    /** A model is currently being loaded into memory. */
    LOADING,

    /** A model is loaded and ready for inference. */
    READY,

    /** The model failed to load or encountered a fatal error. */
    ERROR
}
