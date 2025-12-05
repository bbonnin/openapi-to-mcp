package io.openapimcp.example.assistant;

import dev.langchain4j.mcp.McpToolProvider;
import dev.langchain4j.mcp.client.DefaultMcpClient;
import dev.langchain4j.mcp.client.McpClient;
import dev.langchain4j.mcp.client.transport.McpTransport;
import dev.langchain4j.mcp.client.transport.http.StreamableHttpMcpTransport;
import dev.langchain4j.model.ollama.OllamaChatModel;
import dev.langchain4j.service.AiServices;

public class InfraAssistantFactory {

    public static InfraAssistant createAssistant(String ollamaUrl, String mcpServerUrl) {
        OllamaChatModel chatModel = OllamaChatModel.builder()
                .baseUrl(ollamaUrl)
                //.modelName("llama3.1")
                .modelName("gpt-oss_t2")
                .temperature(0.0)
                .build();

        McpTransport transport = StreamableHttpMcpTransport.builder()
                .url(mcpServerUrl)
                .logRequests(false)
                .logResponses(false)
                .build();

        McpClient mcpClient = DefaultMcpClient.builder()
                .transport(transport)
                .build();

        McpToolProvider toolProvider = McpToolProvider.builder()
                .mcpClients(mcpClient)
                .build();

        return AiServices.builder(InfraAssistant.class)
                .chatModel(chatModel)
                .toolProvider(toolProvider)
                .build();
    }
}
