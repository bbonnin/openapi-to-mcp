package io.openapimcp.server.utils;

import io.modelcontextprotocol.spec.McpSchema;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public class JsonSchemaMapper {

    /** Concert a JSON schema into a map containing the data types. */
    public static Map<String, Object> toMap(McpSchema.JsonSchema schema) {
        Map<String, Object> map = new LinkedHashMap<>();

        putIfNotNull(map, "type", schema.type());
        putIfNotEmpty(map, "required", schema.required());
        putIfNotEmpty(map, "properties", convertMap(schema.properties()));
        putIfNotNull(map, "additionalProperties", schema.additionalProperties());
        putIfNotEmpty(map, "$defs", convertMap(schema.definitions()));

        return map;
    }

    private static void putIfNotNull(Map<String, Object> map, String key, Object value) {
        if (value != null) map.put(key, value);
    }

    private static void putIfNotEmpty(Map<String, Object> map, String key, Map<String, Object> value) {
        if (value != null && !value.isEmpty()) map.put(key, value);
    }

    private static void putIfNotEmpty(Map<String, Object> map, String key, List<String> value) {
        if (value != null && !value.isEmpty()) map.put(key, value);
    }

    private static Map<String, Object> convertMap(Map<String, ?> input) {
        if (input == null || input.isEmpty()) return null;
        Map<String, Object> result = new LinkedHashMap<>();
        for (var entry : input.entrySet()) {
            result.put(entry.getKey(), convertValue(entry.getValue()));
        }
        return result;
    }

    /**
     * Convert a value that may be a MCP JsonSchema, a map (nested), a simple type (string, number, boolean, list, ...).
     */
    private static Object convertValue(Object value) {
        switch (value) {
            case null -> {
                return null;
            }
            case McpSchema.JsonSchema js -> {
                return toMap(js);
            }
            case Map<?, ?> rawMap -> {
                Map<String, Object> nested = new LinkedHashMap<>();
                for (Map.Entry<?, ?> e : rawMap.entrySet()) {
                    nested.put(String.valueOf(e.getKey()), convertValue(e.getValue()));
                }
                return nested;
            }
            case Iterable<?> iterable -> {
                List<Object> list = new ArrayList<>();
                for (Object item : iterable) {
                    list.add(convertValue(item));
                }
                return list;
            }
            default -> {
            }
        }

        return value;
    }
}
