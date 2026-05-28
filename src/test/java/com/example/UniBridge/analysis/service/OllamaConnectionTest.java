package com.example.UniBridge.analysis.service;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

@Tag("ollama")
@SpringBootTest(properties = {
        "spring.ai.ollama.base-url=${OLLAMA_BASE_URL:http://localhost:11434}",
        "spring.ai.ollama.chat.options.model=${OLLAMA_MODEL:gemma3:1b}",
        "spring.ai.ollama.chat.options.temperature=0"
})
class OllamaConnectionTest {

    @Autowired
    private ChatClient.Builder chatClientBuilder;

    @Test
    void ollamaChatClient_returnsExpectedConnectionCheckResponse() {
        String response = chatClientBuilder.build()
                .prompt()
                .user("""
                        This is a connection test.
                        Reply with exactly this token and no other text: OLLAMA_OK
                        """)
                .call()
                .content();

        System.out.println("[OllamaConnectionTest] response = " + response);

        assertThat(response)
                .isNotBlank()
                .contains("OLLAMA_OK");
    }
}
