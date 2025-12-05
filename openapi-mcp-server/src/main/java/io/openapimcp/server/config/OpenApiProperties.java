package io.openapimcp.server.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "openapi")
public class OpenApiProperties {

    /**
     * OpenAPI location(URL, file:, classpath:, ...).
     */
    private String location;

    /**
     * Base URL for HTTP calls.
     */
    private String baseUrl;

    public String getLocation() {
        return location;
    }

    public void setLocation(String location) {
        this.location = location;
    }

    public String getBaseUrl() {
        return baseUrl;
    }

    public void setBaseUrl(String baseUrl) {
        this.baseUrl = baseUrl;
    }
}
