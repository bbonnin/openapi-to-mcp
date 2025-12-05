package io.openapimcp.server.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.modelcontextprotocol.server.McpServerFeatures;
import io.modelcontextprotocol.spec.McpSchema;
import io.openapimcp.server.utils.OpenApiToMcpSchemaConverter;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.Operation;
import io.swagger.v3.oas.models.PathItem;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Build MCP Tools from an OpenAPI definition + wire them to RemoteApiExecutor.
 */
@Service
public class SwaggerToolsFactory {

    private final OpenAPI openAPI;
    private final OpenApiToMcpSchemaConverter converter;
    private final RemoteApiExecutor executor;
    private final ObjectMapper mapper;

    public SwaggerToolsFactory(OpenAPI openAPI, RemoteApiExecutor executor, ObjectMapper mapper) {
        this.openAPI = openAPI;
        this.executor = executor;
        this.mapper = mapper;
        this.converter = new OpenApiToMcpSchemaConverter(openAPI);
    }

    /**
     * Build all SyncToolSpecifications from OpenAPI paths.
     */
    public List<McpServerFeatures.SyncToolSpecification> buildTools() {
        List<McpServerFeatures.SyncToolSpecification> tools = new ArrayList<>();

        openAPI.getPaths().forEach((path, pathItem) -> {
            pathItem.readOperationsMap().forEach((method, operation) -> {
                tools.add(createTool(path, pathItem, method, operation));
            });
        });

        return tools;
    }

    private McpServerFeatures.SyncToolSpecification createTool(
            String path,
            PathItem pathItem,
            PathItem.HttpMethod httpMethod,
            Operation operation) {

        String name = operation.getOperationId() != null && !operation.getOperationId().isBlank()
                ? operation.getOperationId()
                : httpMethod.name().toLowerCase() + "_" + path.replace("/", "_");

        String title = operation.getSummary() != null && !operation.getSummary().isBlank()
                ? operation.getSummary()
                : name;

        String description = operation.getDescription() != null && !operation.getDescription().isBlank()
                ? operation.getDescription()
                : title;

        McpSchema.JsonSchema inputSchema =
                converter.buildInputSchema(path, pathItem, operation);

        Map<String, Object> outputSchema =
                converter.buildOutputSchema(operation);

        McpSchema.Tool tool = new McpSchema.Tool(
                name,
                title,
                description,
                inputSchema,
                outputSchema,
                null, // No annotations
                Map.of()        // Empty meta
        );

        return McpServerFeatures.SyncToolSpecification.builder()
                .tool(tool)
                .callHandler((exchange, request) ->
                        executeTool(path, httpMethod, operation, request.arguments()))
                .build();
    }

    private McpSchema.CallToolResult executeTool(
            String pathTemplate,
            PathItem.HttpMethod httpMethod,
            Operation operation,
            Map<String, Object> arguments) {

        Map<String, Object> args = arguments != null ? arguments : Map.of();

        Object pathObj = args.getOrDefault("path", Map.of());
        Object queryObj = args.getOrDefault("query", Map.of());
        Object bodyObj = args.get("body");

        Map<String, Object> pathArgs = safeToStringObjectMap(pathObj);
        Map<String, Object> queryArgs = safeToStringObjectMap(queryObj);

        String finalUrl = buildFinalUrl(pathTemplate, pathArgs, queryArgs);

        // Remote HTTP call
        RemoteApiResponse response = executor.executeApiCall(finalUrl, httpMethod.name(), bodyObj);

        Object structured;

        try {
            structured = mapper.readValue(response.body(), Object.class);
        } catch (Exception e) {
            structured = null;
        }

        return McpSchema.CallToolResult.builder()
                //.addTextContent(response.body())
                .structuredContent(structured)
                .isError(response.status() >= 400)
                .build();
    }

    private String buildFinalUrl(String pathTemplate, Map<String, Object> pathArgs, Map<String, Object> queryArgs) {
        String resolvedPath = pathTemplate;
        for (var entry : pathArgs.entrySet()) {
            String token = "{" + entry.getKey() + "}";
            resolvedPath = resolvedPath.replace(token, String.valueOf(entry.getValue()));
        }

        StringBuilder url = new StringBuilder();

        String baseUrl = openAPI.getServers() != null && !openAPI.getServers().isEmpty()
                ? openAPI.getServers().get(0).getUrl()
                : "";

        url.append(baseUrl);

        if (!resolvedPath.startsWith("/")) {
            url.append("/");
        }
        url.append(resolvedPath);

        if (!queryArgs.isEmpty()) {
            StringBuilder qs = new StringBuilder();
            boolean first = true;
            for (var entry : queryArgs.entrySet()) {
                if (!first) {
                    qs.append("&");
                }
                first = false;
                qs.append(entry.getKey())
                        .append("=")
                        .append(entry.getValue());
            }
            url.append("?").append(qs);
        }

        return url.toString();
    }

    private Map<String, Object> safeToStringObjectMap(Object value) {
        if (value == null) {
            return Map.of();
        }
        if (value instanceof Map<?, ?> map) {
            Map<String, Object> result = new LinkedHashMap<>();
            map.forEach((k, v) -> {
                if (k != null) {
                    result.put(String.valueOf(k), v);
                }
            });
            return result;
        }
        return Map.of();
    }
}
