package io.openapimcp.server.config;

import io.swagger.parser.OpenAPIParser;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.parser.core.models.SwaggerParseResult;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class OpenApiConfig {

    private static final Logger log = LoggerFactory.getLogger(OpenApiConfig.class);

    @Bean
    public OpenAPI openApiDefinition(OpenApiProperties props) {
        String location = props.getLocation();
        if (location == null || location.isBlank()) {
            throw new IllegalStateException("Property 'openapi.location' must be set");
        }

        SwaggerParseResult result = new OpenAPIParser().readLocation(location, null, null);

        if (result.getMessages() != null && !result.getMessages().isEmpty()) {
            log.warn("OpenAPI parse messages: {}", result.getMessages());
        }

        OpenAPI openAPI = result.getOpenAPI();
        if (openAPI == null) {
            throw new IllegalStateException("Failed to parse OpenAPI: " + location);
        }

        log.info("OpenAPI loaded. Title: {}, Version: {}",
                openAPI.getInfo() != null ? openAPI.getInfo().getTitle() : "N/A",
                openAPI.getInfo() != null ? openAPI.getInfo().getVersion() : "N/A");

        return openAPI;
    }
}
