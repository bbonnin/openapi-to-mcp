package io.openapimcp.server.config;

import io.modelcontextprotocol.server.McpServerFeatures;
import io.openapimcp.server.service.SwaggerToolsFactory;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.List;

@Configuration
public class McpToolsConfig {

    @Bean
    public List<McpServerFeatures.SyncToolSpecification> openApiTools(SwaggerToolsFactory factory) {
        return factory.buildTools();
    }
}
