package io.openapimcp.example.api;

import io.javalin.Javalin;
import io.javalin.openapi.OpenApiInfo;
import io.javalin.openapi.OpenApiServer;
import io.javalin.openapi.plugin.OpenApiPlugin;
import io.javalin.openapi.plugin.swagger.SwaggerPlugin;

public class InfrastructureApiServer {

    public static void main(String[] args) {

        Javalin app = Javalin.create(config -> {
                    config.registerPlugin(new OpenApiPlugin(pluginConfig -> {
                        pluginConfig.withDefinitionConfiguration((version, definition) -> {
                            definition.withInfo((OpenApiInfo info) -> {
                                info.setTitle("Infrastructure API");
                                info.setVersion("1.0.0");
                            });
                            definition.withServer((OpenApiServer server) -> {
                                server.setUrl("http://localhost:7070");
                            });
                        });
                    }));

                    config.registerPlugin(new SwaggerPlugin(swaggerConfig -> {
                        swaggerConfig.setUiPath("/swagger-ui");
                    }));
                });

        app.get("/vms", VmController::listVms);
        app.post("/vms", VmController::createVm);
        app.post("/vms/{name}/start", VmController::startVm);

        app.start(7070);
    }
}
