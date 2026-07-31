package com.ezdo.config;

import com.ezdo.service.McpToolService;
import org.springframework.ai.tool.ToolCallbackProvider;
import org.springframework.ai.tool.method.MethodToolCallbackProvider;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Registers {@link McpToolService} as a {@link ToolCallbackProvider} so that
 * the Spring AI MCP server auto-configuration picks up all {@code @Tool}
 * methods and exposes them over the MCP protocol.
 */
@Configuration
public class McpConfig {

    @Bean
    public ToolCallbackProvider ezdoToolCallbackProvider(McpToolService mcpToolService) {
        return MethodToolCallbackProvider.builder()
                .toolObjects(mcpToolService)
                .build();
    }
}
