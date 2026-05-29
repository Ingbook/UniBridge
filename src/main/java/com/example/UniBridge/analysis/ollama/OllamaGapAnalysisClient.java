package com.example.UniBridge.analysis.ollama;

import com.example.UniBridge.analysis.dto.OllamaGapAnalysisResult;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.time.Duration;
import java.util.Map;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.RequiredArgsConstructor;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

@Component
@RequiredArgsConstructor
public class OllamaGapAnalysisClient {

    private final OllamaProperties ollamaProperties;
    private final ObjectMapper objectMapper = new ObjectMapper();

    public OllamaGapAnalysisResult analyze(String prompt) {
        SimpleClientHttpRequestFactory requestFactory = new SimpleClientHttpRequestFactory();
        requestFactory.setConnectTimeout(Duration.ofSeconds(ollamaProperties.getConnectTimeoutSeconds()));
        requestFactory.setReadTimeout(Duration.ofSeconds(ollamaProperties.getReadTimeoutSeconds()));

        RestClient restClient = RestClient.builder()
                .baseUrl(ollamaProperties.getBaseUrl())
                .requestFactory(requestFactory)
                .build();

        OllamaGenerateResponse response = restClient.post()
                .uri("/api/generate")
                .body(Map.of(
                        "model", ollamaProperties.getModel(),
                        "prompt", prompt,
                        "format", "json",
                        "stream", false
                ))
                .retrieve()
                .body(OllamaGenerateResponse.class);

        String responseText = response == null ? null : response.getResponse();
        return parseResponse(responseText);
    }

    public OllamaGapAnalysisResult parseResponse(String response) {
        if (response == null || response.isBlank()) {
            throw new IllegalArgumentException("Ollama 응답이 비어 있습니다.");
        }

        try {
            return objectMapper.readValue(extractJson(response), OllamaGapAnalysisResult.class);
        } catch (JsonProcessingException e) {
            throw new IllegalArgumentException("Ollama 응답 JSON을 해석할 수 없습니다.", e);
        }
    }

    public String extractJson(String response) {
        int start = response.indexOf('{');
        int end = response.lastIndexOf('}');
        if (start >= 0 && end > start) {
            return response.substring(start, end + 1);
        }
        return response;
    }

    @Getter
    @NoArgsConstructor
    @JsonIgnoreProperties(ignoreUnknown = true)
    private static class OllamaGenerateResponse {

        private String response;
    }
}
