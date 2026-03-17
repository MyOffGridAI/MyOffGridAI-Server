package com.myoffgridai.ai.service;

import java.util.List;

/**
 * Factory for creating {@link ProcessBuilder} instances.
 *
 * <p>Enables testability by allowing injection of a mock that returns
 * a controlled process instead of launching a real OS process.</p>
 */
@FunctionalInterface
public interface ProcessBuilderFactory {

    /**
     * Creates a new {@link ProcessBuilder} with the given command.
     *
     * @param command the command and arguments
     * @return a configured ProcessBuilder
     */
    ProcessBuilder create(List<String> command);
}
