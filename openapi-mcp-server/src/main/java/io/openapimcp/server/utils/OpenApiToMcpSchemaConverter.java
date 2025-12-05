package io.openapimcp.server.utils;

import io.modelcontextprotocol.spec.McpSchema;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.Operation;
import io.swagger.v3.oas.models.PathItem;
import io.swagger.v3.oas.models.media.MediaType;
import io.swagger.v3.oas.models.media.Schema;
import io.swagger.v3.oas.models.parameters.Parameter;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Convert OpenAPI 3.x models into MCP JsonSchemas (inputSchema + outputSchema).
 */
public class OpenApiToMcpSchemaConverter {

    private final OpenAPI openAPI;

    public OpenApiToMcpSchemaConverter(OpenAPI openAPI) {
        this.openAPI = openAPI;
    }

    /**
     * Build MCP input JsonSchema for a given operation.
     */
    public McpSchema.JsonSchema buildInputSchema(
            String path,
            PathItem pathItem,
            Operation operation) {

        List<Parameter> allParams = new ArrayList<>();

        if (pathItem.getParameters() != null) {
            allParams.addAll(pathItem.getParameters());
        }
        if (operation.getParameters() != null) {
            allParams.addAll(operation.getParameters());
        }

        Map<String, Object> pathProps = new LinkedHashMap<>();
        List<String> pathRequired = new ArrayList<>();

        Map<String, Object> queryProps = new LinkedHashMap<>();
        List<String> queryRequired = new ArrayList<>();

        for (Parameter p : allParams) {
            if (p.getIn() == null) {
                continue;
            }
            Schema<?> paramSchema = p.getSchema();
            Map<String, Object> jsonSchema = convertSchemaToMap(paramSchema, new HashSet<>());

            switch (p.getIn()) {
                case "path" -> {
                    pathProps.put(p.getName(), jsonSchema);
                    if (Boolean.TRUE.equals(p.getRequired())) {
                        pathRequired.add(p.getName());
                    }
                }
                case "query" -> {
                    queryProps.put(p.getName(), jsonSchema);
                    if (Boolean.TRUE.equals(p.getRequired())) {
                        queryRequired.add(p.getName());
                    }
                }
                default -> {
                    // Ignore at the moment
                }
            }
        }

        Map<String, Object> pathObject = Map.of(
                "type", "object",
                "properties", pathProps,
                "required", pathRequired,
                "additionalProperties", false
        );

        Map<String, Object> queryObject = Map.of(
                "type", "object",
                "properties", queryProps,
                "required", queryRequired,
                "additionalProperties", false
        );

        Map<String, Object> bodyObject = null;
        boolean bodyRequired = false;

        if (operation.getRequestBody() != null
                && operation.getRequestBody().getContent() != null) {

            MediaType mediaType = operation.getRequestBody()
                    .getContent()
                    .get("application/json");

            if (mediaType != null && mediaType.getSchema() != null) {
                bodyObject = convertSchemaToMap(mediaType.getSchema(), new HashSet<>());
                bodyRequired = Boolean.TRUE.equals(operation.getRequestBody().getRequired());
            }
        }

        Map<String, Object> rootProps = new LinkedHashMap<>();
        rootProps.put("path", pathObject);
        rootProps.put("query", queryObject);

        List<String> rootRequired = new ArrayList<>();

        if (bodyObject != null) {
            rootProps.put("body", bodyObject);
            if (bodyRequired) {
                rootRequired.add("body");
            }
        }

        return new McpSchema.JsonSchema(
                "object",
                rootProps,
                rootRequired,
                true,
                Map.of(),       // $defs
                Map.of()        // definitions
        );
    }

    /**
     * Build a JSON Schema map for the operation output.
     */
    public Map<String, Object> buildOutputSchema(Operation operation) {
        if (operation.getResponses() == null || operation.getResponses().isEmpty()) {
            return null;
        }

        Schema<?> responseSchema = null;

        for (var entry : operation.getResponses().entrySet()) {
            String status = entry.getKey();
            if (!status.startsWith("2")) {
                continue;
            }
            var apiResponse = entry.getValue();
            if (apiResponse.getContent() == null) {
                continue;
            }
            MediaType mediaType = apiResponse.getContent().get("application/json");
            if (mediaType != null && mediaType.getSchema() != null) {
                responseSchema = mediaType.getSchema();
                break;
            }
        }

        if (responseSchema == null) {
            return null;
        }

        return convertSchemaToMap(responseSchema, new HashSet<>());
    }

    private Map<String, Object> convertSchemaToMap(Schema<?> schema, Set<String> visitedRefs) {

        if (schema == null) {
            return Map.of("type", "object");
        }

        if (schema.get$ref() != null) {
            String ref = schema.get$ref();
            if (visitedRefs.contains(ref)) {
                return Map.of("type", "object");
            }
            visitedRefs.add(ref);

            Schema<?> resolved = resolveRef(ref);
            if (resolved == null) {
                return Map.of("type", "object");
            }
            return convertSchemaToMap(resolved, visitedRefs);
        }

        Map<String, Object> result = new LinkedHashMap<>();

        String type = schema.getType();
        if (type == null) {
            type = "object";
        }
        result.put("type", type);

        if (schema.getFormat() != null) {
            result.put("format", schema.getFormat());
        }

        if (schema.getEnum() != null && !schema.getEnum().isEmpty()) {
            result.put("enum", schema.getEnum());
        }

        switch (type) {
            case "object" -> {
                Map<String, Object> props = new LinkedHashMap<>();
                if (schema.getProperties() != null) {
                    schema.getProperties().forEach((name, sub) ->
                            props.put(name, convertSchemaToMap((Schema<?>) sub, visitedRefs)));
                }
                result.put("properties", props);

                if (schema.getRequired() != null) {
                    result.put("required", schema.getRequired());
                } else {
                    result.put("required", List.of());
                }

                Object addProps = schema.getAdditionalProperties();
                if (addProps instanceof Boolean b) {
                    result.put("additionalProperties", b);
                } else {
                    result.put("additionalProperties", false);
                }
            }
            case "array" -> {
                if (schema.getItems() != null) {
                    result.put("items", convertSchemaToMap(schema.getItems(), visitedRefs));
                }
            }
            default -> {
                // Simple types : string, number, integer, boolean...
            }
        }

        return result;
    }

    private Schema<?> resolveRef(String ref) {
        if (openAPI.getComponents() == null
                || openAPI.getComponents().getSchemas() == null) {
            return null;
        }

        // example: "#/components/schemas/MyType"
        int idx = ref.lastIndexOf('/');
        if (idx < 0 || idx + 1 >= ref.length()) {
            return null;
        }

        String name = ref.substring(idx + 1);
        return openAPI.getComponents().getSchemas().get(name);
    }
}
