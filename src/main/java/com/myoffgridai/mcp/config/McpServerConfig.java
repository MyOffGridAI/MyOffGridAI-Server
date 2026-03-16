package com.myoffgridai.mcp.config;

import com.myoffgridai.mcp.service.McpToolsService;
import org.springframework.ai.tool.ToolCallbackProvider;
import org.springframework.ai.tool.method.MethodToolCallbackProvider;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Configuration for the Spring AI MCP server.
 *
 * <p>Registers the {@link McpToolsService} methods annotated with {@code @Tool}
 * as MCP-callable tools via the {@link ToolCallbackProvider} mechanism.
 * The MCP server auto-configuration discovers this provider and exposes
 * the tools over the SSE transport at {@code /mcp/sse}.</p>
 *
 * @author MyOffGridAI
 * @since 0.1.0
 */
@Configuration
public class McpServerConfig {

    /**
     * Creates a {@link ToolCallbackProvider} that scans the {@link McpToolsService}
     * for {@code @Tool}-annotated methods and registers them as MCP tools.
     *
     * @param mcpToolsService the service containing MCP tool methods
     * @return the tool callback provider
     */
    @Bean
    public ToolCallbackProvider mcpToolCallbackProvider(McpToolsService mcpToolsService) {
        return MethodToolCallbackProvider.builder()
                .toolObjects(mcpToolsService)
                .build();
    }
}
