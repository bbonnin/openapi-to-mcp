package io.openapimcp.example.api;

import io.javalin.openapi.OpenApiByFields;

@OpenApiByFields
public class VmMetrics {
    public float cpuLoad;
    public float diskUsagePercent;

    public VmMetrics() {
    }

    public VmMetrics(float cpuLoad, float diskUsagePercent) {
        this.cpuLoad = cpuLoad;
        this.diskUsagePercent = diskUsagePercent;
    }
}
