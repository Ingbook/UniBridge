package com.example.UniBridge.analysis.ollama;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.example.UniBridge.analysis.dto.OllamaGapAnalysisResult;
import org.junit.jupiter.api.Test;

class OllamaGapAnalysisClientTest {

    private final OllamaGapAnalysisClient client = new OllamaGapAnalysisClient(
            new OllamaProperties()
    );

    @Test
    void extractJson_returnsJsonOnly_whenResponseContainsText() {
        String response = "prefix {\"totalScore\":82,\"summary\":\"ok\"} suffix";

        String json = client.extractJson(response);

        assertThat(json).isEqualTo("{\"totalScore\":82,\"summary\":\"ok\"}");
    }

    @Test
    void parseResponse_mapsJsonToDto() {
        String response = """
                ```json
                {
                  "totalScore": 82,
                  "scoreDescription": "상위 동문 평균과 가까운 준비도입니다.",
                  "summary": "직무 경험은 좋고 프로젝트 설명 보강이 필요합니다.",
                  "items": [
                    {
                      "category": "GPA",
                      "userValue": "3.8/4.5",
                      "alumnusValue": "4.2/4.5",
                      "gapDescription": "동문보다 0.4 낮습니다.",
                      "aiScore": 78,
                      "status": "NEEDS_IMPROVEMENT"
                    }
                  ],
                  "strengths": ["지원 직무와 연결되는 경험이 명확합니다."],
                  "weaknesses": ["프로젝트 성과와 문제 해결 과정 설명이 부족합니다."],
                  "comments": [
                    "현재 스펙은 기본 경쟁력을 갖추고 있습니다.",
                    "다만 프로젝트 설명을 수치와 결과 중심으로 보완해야 합니다.",
                    "동문 대비 어학성적과 포트폴리오 완성도를 우선 개선하는 것이 좋습니다."
                  ]
                }
                ```
                """;

        OllamaGapAnalysisResult result = client.parseResponse(response);

        assertThat(result.getTotalScore()).isEqualTo(82);
        assertThat(result.getItems()).hasSize(1);
        assertThat(result.getStrengths()).contains("지원 직무와 연결되는 경험이 명확합니다.");
        assertThat(result.getComments()).hasSize(3);
    }

    @Test
    void parseResponse_throwsException_whenJsonIsInvalid() {
        assertThatThrownBy(() -> client.parseResponse("not-json"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Ollama 응답 JSON을 해석할 수 없습니다.");
    }
}
