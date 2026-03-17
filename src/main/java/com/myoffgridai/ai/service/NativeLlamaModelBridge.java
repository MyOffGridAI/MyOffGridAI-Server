package com.myoffgridai.ai.service;

import de.kherud.llama.InferenceParameters;
import de.kherud.llama.LlamaOutput;

/**
 * Abstraction over the native {@link de.kherud.llama.LlamaModel} for testability.
 *
 * <p>The JNI-backed {@code LlamaModel} loads a native shared library when its class
 * is first referenced by the classloader. This interface decouples the service from
 * that concrete class so that tests can mock inference behaviour without triggering
 * native-library initialisation (which crashes the JVM in test environments where
 * the native binary is not available).</p>
 *
 * @see NativeLlamaInferenceService
 */
interface NativeLlamaModelBridge extends AutoCloseable {

    /**
     * Runs synchronous (blocking) text completion.
     *
     * @param params inference parameters including the prompt
     * @return the complete generated text
     */
    String complete(InferenceParameters params);

    /**
     * Runs streaming text generation, returning tokens one at a time.
     *
     * @param params inference parameters including the prompt
     * @return an iterable of output tokens
     */
    Iterable<LlamaOutput> generate(InferenceParameters params);

    /**
     * Computes an embedding vector for the given text.
     *
     * @param text the text to embed
     * @return the embedding as a float array
     */
    float[] embed(String text);

    /**
     * Releases native resources held by the underlying model.
     */
    @Override
    void close();
}
