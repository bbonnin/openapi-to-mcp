package io.openapimcp.example.api;

import io.javalin.http.Context;
import io.javalin.openapi.HttpMethod;
import io.javalin.openapi.OpenApi;
import io.javalin.openapi.OpenApiContent;
import io.javalin.openapi.OpenApiParam;
import io.javalin.openapi.OpenApiRequestBody;
import io.javalin.openapi.OpenApiResponse;

public class VmController {

    @OpenApi(
            summary = "Create a new virtual machine",
            operationId = "createVm",
            path = "/vms",
            methods = HttpMethod.POST,
            tags = {"VM"},
            requestBody = @OpenApiRequestBody(
                    required = true,
                    content = { @OpenApiContent(from = CreateVmRequest.class) }),
            responses = {
                    @OpenApiResponse(
                            status = "201",
                            content = { @OpenApiContent(from = VirtualMachine.class) }),
                    @OpenApiResponse(status = "400")
            }
    )
    public static void createVm(Context ctx) {
        CreateVmRequest req = ctx.bodyAsClass(CreateVmRequest.class);
        if (req.name == null || req.name.isBlank()) {
            ctx.status(400).json(new ErrorResponse("Name is required"));
            return;
        }
        VirtualMachine vm = VmRepository.addVm(req.name);
        ctx.json(vm);
    }

    @OpenApi(
            summary = "List the virtual machines with some metrics (cpu load, disk usage)",
            operationId = "listVms",
            path = "/vms",
            methods = HttpMethod.GET,
            tags = {"VM"},
            responses = {
                    @OpenApiResponse(
                            status = "200",
                            content = { @OpenApiContent(from = VirtualMachine[].class) })
            }
    )
    public static void listVms(Context ctx) {
        ctx.json(VmRepository.listAll());
    }

    @OpenApi(
            summary = "Start a VM by name",
            operationId = "startVm",
            path = "/vms/{name}/start",
            methods = HttpMethod.POST,
            tags = {"VM"},
            pathParams = {
                    @OpenApiParam(name = "name", description = "Name of the VM")
            },
            responses = {
                    @OpenApiResponse(
                            status = "200",
                            description = "VM started (status: STARTING)",
                            content = {@OpenApiContent(from = VirtualMachine.class)}
                    ),
                    @OpenApiResponse(
                            status = "404",
                            description = "VM not found"
                    )
            }
    )
    public static void startVm(Context ctx) {
        String name = ctx.pathParam("name");
        VirtualMachine vm = VmRepository.start(name);
        if (vm == null) {
            ctx.status(404).json(new ErrorResponse("VM not found"));
        } else {
            ctx.json(vm);
        }
    }

    public static class ErrorResponse {
        public String error;

        public ErrorResponse() {
        }

        public ErrorResponse(String error) {
            this.error = error;
        }
    }
}
