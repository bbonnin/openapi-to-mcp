package io.openapimcp.example.api;

import io.javalin.openapi.OpenApiByFields;

@OpenApiByFields
public class CreateVmRequest {
    public String name;

    public CreateVmRequest() {
    }

    public CreateVmRequest(String name) {
        this.name = name;
    }
}
