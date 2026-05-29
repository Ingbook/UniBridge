package com.example.UniBridge.analysis.ollama;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Getter
@Setter
@Component
@ConfigurationProperties(prefix = "unibridge.ollama")
public class OllamaProperties {

    private String baseUrl = "http://localhost:11434";
    private String model = "gemma3:1b";
    private int connectTimeoutSeconds = 5;
    private int readTimeoutSeconds = 120;
}
