package com.example.UniBridge.analysis.service;

import com.example.UniBridge.analysis.dto.AiAnalysisResult;
import com.example.UniBridge.certification.entity.Certification;
import com.example.UniBridge.certification.entity.UserCertification;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class LocalAiAnalysisService {

    private static final String DEFAULT_SUMMARY = "서버가 계산한 기본 점수를 기준으로 목표 기업과의 차이를 분석했습니다.";
    private static final String DEFAULT_RECOMMENDATION = "학점과 직무 관련 자격증 중 부족한 항목을 우선 보완해 주세요.";

    private final ObjectProvider<ChatClient.Builder> chatClientBuilderProvider;
    private final ObjectMapper objectMapper = new ObjectMapper();

    @Value("${unibridge.ai.enabled:true}")
    private boolean aiEnabled;

    public AiAnalysisResult analyzeSpec(
            int gpaScore,
            int certificationScore,
            int totalScore,
            int targetAverageScore,
            int gapScore,
            List<UserCertification> userCertifications
    ) {
        try {
            if (!aiEnabled) {
                return fallbackResult(gpaScore, certificationScore, totalScore, targetAverageScore, gapScore);
            }

            ChatClient.Builder builder = chatClientBuilderProvider.getIfAvailable();
            if (builder == null) {
                return fallbackResult(gpaScore, certificationScore, totalScore, targetAverageScore, gapScore);
            }

            String response = builder.build()
                    .prompt()
                    .user(createPrompt(gpaScore, certificationScore, totalScore, targetAverageScore, gapScore,
                            userCertifications))
                    .call()
                    .content();

            return validateResult(parseResponse(response));
        } catch (Exception e) {
            return fallbackResult(gpaScore, certificationScore, totalScore, targetAverageScore, gapScore);
        }
    }

    AiAnalysisResult validateResult(AiAnalysisResult result) {
        if (result == null) {
            return fallbackResult(0, 0, 0, 0, 0);
        }

        return AiAnalysisResult.builder()
                .adjustmentScore(clamp(result.getAdjustmentScore() == null ? 0 : result.getAdjustmentScore(), -10, 10))
                .summary(hasText(result.getSummary()) ? result.getSummary().trim() : DEFAULT_SUMMARY)
                .recommendation(hasText(result.getRecommendation())
                        ? result.getRecommendation().trim()
                        : DEFAULT_RECOMMENDATION)
                .build();
    }

    private AiAnalysisResult parseResponse(String response) throws Exception {
        if (!hasText(response)) {
            return null;
        }

        String json = extractJson(response);
        return objectMapper.readValue(json, AiAnalysisResult.class);
    }

    private String extractJson(String response) {
        int start = response.indexOf('{');
        int end = response.lastIndexOf('}');
        if (start >= 0 && end > start) {
            return response.substring(start, end + 1);
        }
        return response;
    }

    private String createPrompt(
            int gpaScore,
            int certificationScore,
            int totalScore,
            int targetAverageScore,
            int gapScore,
            List<UserCertification> userCertifications
    ) {
        return """
                당신은 취업 스펙 분석 보조 AI입니다.
                서버가 이미 계산한 점수를 신뢰하고, 기본 총점을 다시 계산하지 마세요.

                입력 정보:
                - 학점 점수: %d
                - 자격증 점수: %d
                - 기본 총점: %d
                - 목표 기업 평균 점수: %d
                - 목표 기업과의 점수 차이: %d
                - 보유 자격증 목록: %s

                규칙:
                - adjustmentScore는 -10에서 +10 사이 정수만 반환하세요.
                - 기본 총점을 다시 계산하지 마세요.
                - 서버가 계산한 점수를 기준으로 분석하세요.
                - 보유 자격증이 직무와 관련성이 높으면 소폭 가산하세요.
                - 학점 점수나 자격증 점수가 낮으면 보완 방향을 제시하세요.
                - summary는 한 문장으로 작성하세요.
                - recommendation은 사용자가 다음에 해야 할 행동을 구체적으로 작성하세요.
                - 반드시 한국어로 작성하세요.
                - 아래 JSON 형식만 반환하고 다른 설명은 포함하지 마세요.

                {
                  "adjustmentScore": 0,
                  "summary": "한 문장 요약",
                  "recommendation": "구체적인 다음 행동"
                }
                """.formatted(gpaScore, certificationScore, totalScore, targetAverageScore, gapScore,
                certificationNames(userCertifications));
    }

    private String certificationNames(List<UserCertification> userCertifications) {
        if (userCertifications == null || userCertifications.isEmpty()) {
            return "없음";
        }

        String names = userCertifications.stream()
                .map(UserCertification::getCertification)
                .filter(Objects::nonNull)
                .map(Certification::getName)
                .filter(this::hasText)
                .collect(Collectors.joining(", "));
        return hasText(names) ? names : "없음";
    }

    private AiAnalysisResult fallbackResult(
            int gpaScore,
            int certificationScore,
            int totalScore,
            int targetAverageScore,
            int gapScore
    ) {
        return AiAnalysisResult.builder()
                .adjustmentScore(0)
                .summary(createFallbackSummary(gpaScore, certificationScore, totalScore, targetAverageScore, gapScore))
                .recommendation(DEFAULT_RECOMMENDATION)
                .build();
    }

    private String createFallbackSummary(Integer gpaScore, Integer certificationScore, Integer totalScore,
                                         Integer targetAverageScore, Integer gapScore) {
        StringBuilder summary = new StringBuilder();
        if (gapScore > 10) {
            summary.append("목표 기업 평균 점수보다 낮습니다. 학점 또는 자격증 보완이 필요합니다.");
        } else if (gapScore >= 1) {
            summary.append("목표 기업 평균에 근접했습니다. 부족한 항목을 조금 더 보완하면 좋습니다.");
        } else if (gapScore == 0) {
            summary.append("목표 기업 평균 점수와 동일한 수준입니다.");
        } else {
            summary.append("목표 기업 평균보다 높은 점수입니다. 현재 스펙은 경쟁력이 있습니다.");
        }

        if (gpaScore < 70) {
            summary.append(" 학점 점수 보완이 필요합니다.");
        }
        if (certificationScore < 40) {
            summary.append(" 직무 관련 자격증을 추가하면 좋습니다.");
        }
        return summary.toString();
    }

    private int clamp(int value, int min, int max) {
        return Math.max(min, Math.min(max, value));
    }

    private boolean hasText(String value) {
        return value != null && !value.isBlank();
    }
}
