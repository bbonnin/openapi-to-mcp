package io.openapimcp.server;

import io.openapimcp.server.config.OpenApiProperties;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;

@SpringBootApplication
@EnableConfigurationProperties(OpenApiProperties.class)
public class OpenApiMcpServer {

    public static void main(String[] args) {
        SpringApplication.run(OpenApiMcpServer.class, args);
    }
}