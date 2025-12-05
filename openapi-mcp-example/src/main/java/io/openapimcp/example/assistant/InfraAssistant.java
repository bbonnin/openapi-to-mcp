package io.openapimcp.example.assistant;

import dev.langchain4j.service.SystemMessage;

public interface InfraAssistant {

    @SystemMessage("""
        You are an expert in infrastructure.
        You have to follow the data format if tools need to be invoked.
        """)
    String chat(String input);
}
