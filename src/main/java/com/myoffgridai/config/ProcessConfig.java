package com.myoffgridai.config;

import com.myoffgridai.ai.service.ProcessBuilderFactory;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Always-active configuration for process management beans.
 *
 * <p>Provides the {@link ProcessBuilderFactory} bean independently of the
 * inference provider selection so that services like
 * {@link com.myoffgridai.ai.judge.JudgeModelProcessService} can always
 * inject it regardless of which provider is active.</p>
 */
@Configuration
public class ProcessConfig {

    /**
     * Factory for creating {@link ProcessBuilder} instances, enabling testability
     * of process management in services that spawn external processes.
     *
     * @return the default ProcessBuilderFactory using the system ProcessBuilder
     */
    @Bean
    public ProcessBuilderFactory processBuilderFactory() {
        return ProcessBuilder::new;
    }
}
