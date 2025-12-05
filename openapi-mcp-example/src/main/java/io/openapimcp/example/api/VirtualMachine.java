package io.openapimcp.example.api;

import io.javalin.openapi.OpenApiByFields;

@OpenApiByFields
public class VirtualMachine {
    public String name;
    public VmStatus status;
    public VmMetrics metrics;

    public VirtualMachine() {
    }

    public VirtualMachine(String name, VmStatus status, VmMetrics metrics) {
        this.name = name;
        this.status = status;
        this.metrics = metrics;
    }
}
