package com.example.UniBridge.analysis.service;

import com.example.UniBridge.analysis.dto.AiAnalysisResult;
import com.example.UniBridge.analysis.dto.AiProfileAnalysisResponse;
import com.example.UniBridge.certification.entity.Certification;
import com.example.UniBridge.certification.entity.UserCertification;
import com.example.UniBridge.company.Company;
import com.example.UniBridge.specification.entity.Specification;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class LocalAiAnalysisService {

    public static final String SOURCE_OLLAMA = "OLLAMA";
    public static final String SOURCE_DISABLED = "DISABLED";
    public static final String SOURCE_UNAVAILABLE = "UNAVAILABLE";
    public static final String SOURCE_FALLBACK = "FALLBACK";

    private static final String DEFAULT_SUMMARY = "서버가 계산한 기본 점수를 기준으로 목표 기업과의 차이를 분석했습니다.";
    private static final String DEFAULT_RECOMMENDATION = "학점과 직무 관련 자격증 중 부족한 항목을 우선 보완해 주세요.";
    private static final List<String> ALLOWED_CERTIFICATIONS = List.of(
            "정보처리기사",
            "SQLD",
            "ADsP",
            "AWS Cloud Practitioner",
            "리눅스마스터 2급",
            "컴퓨터활용능력 1급"
    );

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
            Specification specification,
            Company company,
            List<UserCertification> userCertifications
    ) {
        try {
            if (!aiEnabled) {
                return fallbackResult(gpaScore, certificationScore, totalScore, targetAverageScore, gapScore,
                        SOURCE_DISABLED);
            }

            ChatClient.Builder builder = chatClientBuilderProvider.getIfAvailable();
            if (builder == null) {
                return fallbackResult(gpaScore, certificationScore, totalScore, targetAverageScore, gapScore,
                        SOURCE_UNAVAILABLE);
            }

            String response = builder.build()
                    .prompt()
                    .user(createPrompt(gpaScore, certificationScore, totalScore, targetAverageScore, gapScore,
                            specification, company, userCertifications))
                    .call()
                    .content();

            return validateResult(parseResponse(response), SOURCE_OLLAMA);
        } catch (Exception e) {
            return fallbackResult(gpaScore, certificationScore, totalScore, targetAverageScore, gapScore,
                    SOURCE_FALLBACK);
        }
    }

    public AiProfileAnalysisResponse.AiAnalysis analyzeProfile(AiProfileAnalysisResponse.UserProfile userProfile) {
        try {
            if (!aiEnabled) {
                return fallbackProfileResult(userProfile, SOURCE_DISABLED);
            }

            ChatClient.Builder builder = chatClientBuilderProvider.getIfAvailable();
            if (builder == null) {
                return fallbackProfileResult(userProfile, SOURCE_UNAVAILABLE);
            }

            String response = builder.build()
                    .prompt()
                    .user(createProfilePrompt(userProfile))
                    .call()
                    .content();

            return validateProfileResult(parseProfileResponse(response), userProfile, SOURCE_OLLAMA);
        } catch (Exception e) {
            return fallbackProfileResult(userProfile, SOURCE_FALLBACK);
        }
    }

    AiAnalysisResult validateResult(AiAnalysisResult result, String analysisSource) {
        if (result == null) {
            return fallbackResult(0, 0, 0, 0, 0, analysisSource);
        }

        return AiAnalysisResult.builder()
                .adjustmentScore(clamp(result.getAdjustmentScore() == null ? 0 : result.getAdjustmentScore(), -10, 10))
                .summary(hasText(result.getSummary()) ? result.getSummary().trim() : DEFAULT_SUMMARY)
                .recommendation(hasText(result.getRecommendation())
                        ? result.getRecommendation().trim()
                        : DEFAULT_RECOMMENDATION)
                .analysisSource(analysisSource)
                .build();
    }

    private AiAnalysisResult parseResponse(String response) throws Exception {
        if (!hasText(response)) {
            return null;
        }

        String json = extractJson(response);
        return objectMapper.readValue(json, AiAnalysisResult.class);
    }

    private AiProfileAnalysisResponse.AiAnalysis parseProfileResponse(String response) throws Exception {
        if (!hasText(response)) {
            return null;
        }

        String json = extractJson(response);
        return objectMapper.readValue(json, AiProfileAnalysisResponse.AiAnalysis.class);
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
            Specification specification,
            Company company,
            List<UserCertification> userCertifications
    ) {
        return """
                당신은 취업 스펙 분석 보조 AI입니다.
                서버가 이미 계산한 점수를 신뢰하고, 기본 총점을 다시 계산하지 마세요.

                입력 정보:
                - 목표 기업명: %s
                - 목표 기업 산업군: %s
                - 목표 기업 주요 직무: %s
                - 사용자 학점: %s / %s
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
                """.formatted(
                company == null ? "알 수 없음" : blankToDefault(company.getName(), "알 수 없음"),
                company == null ? "알 수 없음" : blankToDefault(company.getIndustry(), "알 수 없음"),
                company == null ? "알 수 없음" : blankToDefault(company.getMainJobRole(), "알 수 없음"),
                specification == null || specification.getGpa() == null ? "알 수 없음" : specification.getGpa(),
                specification == null || specification.getMaxGpa() == null ? "알 수 없음" : specification.getMaxGpa(),
                gpaScore, certificationScore, totalScore, targetAverageScore, gapScore,
                certificationNames(userCertifications));
    }

    private String createProfilePrompt(AiProfileAnalysisResponse.UserProfile userProfile) throws JsonProcessingException {
        Map<String, Object> input = new LinkedHashMap<>();
        input.put("사용자 이름", userProfile.getName());
        input.put("학점", userProfile.getGpa() == null ? "미입력" : userProfile.getGpa() + " / 4.5");
        input.put("어학 종류", userProfile.getLanguage() == null ? "미입력" : userProfile.getLanguage().getType());
        input.put("어학 점수", userProfile.getLanguage() == null ? "미입력" : userProfile.getLanguage().getScore());
        input.put("어학 표시값", userProfile.getLanguage() == null ? "미입력" : userProfile.getLanguage().getDisplayText());
        input.put("자격증 목록", certificationItems(userProfile));
        input.put("자격증 개수", certificationCount(userProfile));
        input.put("수상경력 개수", userProfile.getAwardCount());
        input.put("프로젝트", blankToDefault(userProfile.getProject(), "미입력"));
        input.put("포트폴리오", blankToDefault(userProfile.getPortfolio(), "미입력"));
        input.put("입력 정보 부족 여부", isProfileInsufficient(userProfile));

        return """
                당신은 취업 준비 프로필을 분석하는 AI입니다. 반드시 JSON만 반환하세요.
                markdown, 코드블록, 추가 설명을 포함하지 마세요.

                사용자 입력 정보:
                %s

                분석 규칙:
                - 모든 응답은 한국어로 작성하세요.
                - 강점, 약점, AI 맞춤형 코멘트를 취업 준비 관점에서 작성하세요.
                - 자격증은 개수뿐 아니라 실제 자격증 이름과 직무 연관성을 기준으로 평가하세요.
                - 자격증 분석 대상은 정보처리기사, SQLD, ADsP, AWS Cloud Practitioner, 리눅스마스터 2급, 컴퓨터활용능력 1급만입니다.
                - 보유 자격증 목록에 포함되지 않은 자격증 이름을 절대 언급하지 마세요.
                - 자격증 개수는 입력 JSON의 "자격증 개수" 값을 그대로 사용하고, 임의로 자격증을 추가하지 마세요.
                - 보유 자격증 목록이 비어 있으면 특정 자격증 이름을 강점이나 코멘트에 쓰지 마세요.
                - 수상경력은 awardCount 개수를 기준으로 평가하세요.
                - 프로젝트는 리스트가 아니라 단일 문자열입니다. 해당 문자열의 내용을 기반으로 분석하세요.
                - 포트폴리오는 리스트가 아니라 단일 문자열입니다. 프로젝트 결과물, 링크, 구성, 본인 기여도, 성과 설명을 기준으로 분석하세요.
                - 프로젝트 문자열이 비어 있거나 부족한 경우 weaknesses에 "프로젝트 설명 보완 필요" 취지를 포함하세요.
                - 포트폴리오 문자열이 비어 있거나 부족한 경우 weaknesses에 "포트폴리오 설명 보완 필요" 취지를 포함하세요.
                - 입력 정보가 부족하면 "입력 정보가 부족하여 정밀 분석에 한계가 있습니다"라는 취지를 comment에 반영하세요.

                응답 JSON 스키마:
                {
                  "strengths": ["강점 1", "강점 2"],
                  "weaknesses": ["약점 1", "약점 2"],
                  "comment": "AI 맞춤형 코멘트"
                }
                """.formatted(objectMapper.writeValueAsString(input));
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
            int gapScore,
            String analysisSource
    ) {
        return AiAnalysisResult.builder()
                .adjustmentScore(0)
                .summary(createFallbackSummary(gpaScore, certificationScore, totalScore, targetAverageScore, gapScore))
                .recommendation(DEFAULT_RECOMMENDATION)
                .analysisSource(analysisSource)
                .build();
    }

    AiProfileAnalysisResponse.AiAnalysis validateProfileResult(AiProfileAnalysisResponse.AiAnalysis result,
                                                               AiProfileAnalysisResponse.UserProfile userProfile,
                                                               String analysisSource) {
        AiProfileAnalysisResponse.AiAnalysis fallback = fallbackProfileResult(userProfile, analysisSource);
        if (result == null) {
            return fallback;
        }

        List<String> strengths = sanitizeProfileItems(result.getStrengths(),
                certificationAwareStrength(userProfile), userProfile);
        List<String> weaknesses = sanitizeProfileItems(result.getWeaknesses(),
                fallback.getWeaknesses().get(0), userProfile);
        String comment = sanitizeProfileComment(result.getComment(), fallback.getComment(), userProfile);

        return AiProfileAnalysisResponse.AiAnalysis.builder()
                .strengths(strengths)
                .weaknesses(weaknesses)
                .comment(comment)
                .build();
    }

    private List<String> sanitizeProfileItems(List<String> values, String defaultValue,
                                              AiProfileAnalysisResponse.UserProfile userProfile) {
        if (values == null || values.isEmpty()) {
            return List.of(defaultValue);
        }
        List<String> normalized = values.stream()
                .filter(this::hasText)
                .map(String::trim)
                .filter(value -> !containsUnownedCertification(value, userProfile))
                .toList();
        return normalized.isEmpty() ? List.of(defaultValue) : normalized;
    }

    private String sanitizeProfileComment(String value, String defaultValue,
                                          AiProfileAnalysisResponse.UserProfile userProfile) {
        String normalized = defaultText(value, defaultValue);
        if (containsUnownedCertification(normalized, userProfile)) {
            return certificationAwareComment(userProfile);
        }
        return normalized;
    }

    private boolean containsUnownedCertification(String value, AiProfileAnalysisResponse.UserProfile userProfile) {
        if (!hasText(value)) {
            return false;
        }
        Set<String> ownedCertifications = Set.copyOf(certificationItems(userProfile));
        return ALLOWED_CERTIFICATIONS.stream()
                .filter(certification -> !ownedCertifications.contains(certification))
                .anyMatch(value::contains);
    }

    private String certificationAwareStrength(AiProfileAnalysisResponse.UserProfile userProfile) {
        List<String> certifications = certificationItems(userProfile);
        if (!certifications.isEmpty()) {
            return "%s 보유로 직무 기초 역량을 보여줄 수 있습니다."
                    .formatted(String.join(", ", certifications));
        }
        return profileStrength(userProfile);
    }

    private String certificationAwareComment(AiProfileAnalysisResponse.UserProfile userProfile) {
        List<String> certifications = certificationItems(userProfile);
        String certificationSummary = certifications.isEmpty()
                ? "현재 입력된 보유 자격증은 없습니다."
                : "현재 보유 자격증은 %s 총 %d개입니다."
                .formatted(String.join(", ", certifications), certifications.size());
        if (hasText(userProfile.getProject())) {
            return certificationSummary
                    + " 프로젝트와 포트폴리오의 사용 기술, 담당 역할, 성과를 구체적으로 보완하면 더 정확한 분석이 가능합니다.";
        }
        return certificationSummary + " 프로젝트와 포트폴리오 설명을 함께 입력하면 더 정확한 분석이 가능합니다.";
    }

    private AiProfileAnalysisResponse.AiAnalysis fallbackProfileResult(AiProfileAnalysisResponse.UserProfile userProfile,
                                                                      String analysisSource) {
        boolean insufficient = isProfileInsufficient(userProfile);
        return AiProfileAnalysisResponse.AiAnalysis.builder()
                .strengths(List.of(profileStrength(userProfile)))
                .weaknesses(List.of(profileWeakness(userProfile, insufficient)))
                .comment(profileComment(userProfile, insufficient, analysisSource))
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

    private String profileStrength(AiProfileAnalysisResponse.UserProfile userProfile) {
        if (hasText(userProfile.getPortfolio())) {
            return "포트폴리오를 통해 프로젝트 결과물과 본인 기여도를 설명할 수 있습니다.";
        }
        if (hasText(userProfile.getProject())) {
            return userProfile.getProject() + " 경험을 통해 직무 역량을 설명할 수 있습니다.";
        }
        if (!certificationItems(userProfile).isEmpty()) {
            return "허용 자격증을 기반으로 직무 기초 역량을 보여줄 수 있습니다.";
        }
        return "입력된 프로필을 기준으로 취업 준비 상태를 점검할 수 있습니다.";
    }

    private String profileWeakness(AiProfileAnalysisResponse.UserProfile userProfile, boolean insufficient) {
        if (insufficient) {
            return "입력 정보가 부족해 정밀한 약점 도출에 한계가 있습니다.";
        }
        if (!hasText(userProfile.getProject())) {
            return "프로젝트 설명 보완 필요: 사용 기술, 담당 역할, 성과를 구체적으로 입력하면 좋습니다.";
        }
        if (!hasText(userProfile.getPortfolio())) {
            return "포트폴리오 설명 보완 필요: 프로젝트 결과물, 링크, 구성, 본인 기여도를 구체적으로 입력하면 좋습니다.";
        }
        if (certificationItems(userProfile).isEmpty()) {
            return "직무 관련 자격증 근거가 부족합니다.";
        }
        return "프로젝트 성과와 포트폴리오 구성 설명을 더 구체화하면 좋습니다.";
    }

    private String profileComment(AiProfileAnalysisResponse.UserProfile userProfile, boolean insufficient,
                                  String analysisSource) {
        if (insufficient) {
            return "입력 정보가 부족하여 정밀 분석에 한계가 있습니다. 프로젝트와 포트폴리오 설명을 보완하면 더 구체적인 맞춤 분석이 가능합니다.";
        }
        if (!hasText(userProfile.getProject())) {
            return "프로젝트 설명 보완 필요: 사용 기술, 담당 역할, 성과를 추가하면 더 좋은 평가를 받을 수 있습니다.";
        }
        if (!hasText(userProfile.getPortfolio())) {
            return "포트폴리오 설명 보완 필요: 프로젝트 결과물, 링크, 구성, 본인 기여도를 추가하면 더 좋은 평가를 받을 수 있습니다.";
        }
        return "현재 프로필은 백엔드/데이터 직무 지원에 활용하기 좋은 구성을 가지고 있습니다. 프로젝트와 포트폴리오에서 사용 기술, 담당 역할, 성과를 구체적으로 보완하면 더 좋은 평가를 받을 수 있습니다.";
    }

    private boolean isProfileInsufficient(AiProfileAnalysisResponse.UserProfile userProfile) {
        return userProfile == null
                || (userProfile.getGpa() == null
                && (userProfile.getLanguage() == null || userProfile.getLanguage().getScore() == null)
                && certificationItems(userProfile).isEmpty()
                && (userProfile.getAwardCount() == null || userProfile.getAwardCount() == 0)
                && !hasText(userProfile.getProject())
                && !hasText(userProfile.getPortfolio()));
    }

    private List<String> certificationItems(AiProfileAnalysisResponse.UserProfile userProfile) {
        if (userProfile == null || userProfile.getCertifications() == null) {
            return List.of();
        }
        return safeList(userProfile.getCertifications().getItems());
    }

    private int certificationCount(AiProfileAnalysisResponse.UserProfile userProfile) {
        if (userProfile == null || userProfile.getCertifications() == null
                || userProfile.getCertifications().getCount() == null) {
            return certificationItems(userProfile).size();
        }
        return userProfile.getCertifications().getCount();
    }

    private List<String> emptyToDefault(List<String> values, String defaultValue) {
        if (values == null || values.isEmpty()) {
            return List.of(defaultValue);
        }
        List<String> normalized = values.stream()
                .filter(this::hasText)
                .map(String::trim)
                .toList();
        return normalized.isEmpty() ? List.of(defaultValue) : normalized;
    }

    private String defaultText(String value, String defaultValue) {
        return hasText(value) ? value.trim() : defaultValue;
    }

    private <T> List<T> safeList(List<T> values) {
        return values == null ? List.of() : values;
    }

    private int clamp(int value, int min, int max) {
        return Math.max(min, Math.min(max, value));
    }

    private boolean hasText(String value) {
        return value != null && !value.isBlank();
    }

    private String blankToDefault(String value, String defaultValue) {
        return hasText(value) ? value : defaultValue;
    }
}
