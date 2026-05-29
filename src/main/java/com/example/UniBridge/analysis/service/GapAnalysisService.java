package com.example.UniBridge.analysis.service;

import com.example.UniBridge.alumnus.Alumnus;
import com.example.UniBridge.alumnus.AlumnusRepository;
import com.example.UniBridge.analysis.dto.AiDetailAnalysisResponse;
import com.example.UniBridge.analysis.dto.ComparisonItemResponse;
import com.example.UniBridge.analysis.dto.ComparisonStatus;
import com.example.UniBridge.analysis.dto.GapAnalysisRequest;
import com.example.UniBridge.analysis.dto.GapAnalysisResponse;
import com.example.UniBridge.analysis.dto.OllamaGapAnalysisResult;
import com.example.UniBridge.analysis.dto.SpecProfileResponse;
import com.example.UniBridge.analysis.ollama.OllamaGapAnalysisClient;
import com.example.UniBridge.certification.repository.UserCertificationRepository;
import com.example.UniBridge.company.Company;
import com.example.UniBridge.company.CompanyRepository;
import com.example.UniBridge.specification.entity.Specification;
import com.example.UniBridge.specification.service.SpecificationService;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.math.BigDecimal;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@RequiredArgsConstructor
public class GapAnalysisService {

    private static final Long CURRENT_USER_ID = 1L;
    private static final String DEFAULT_USER_NAME = "현재 사용자";
    private static final String DEFAULT_LANGUAGE_TYPE = "TOEIC";
    private static final Integer DEFAULT_LANGUAGE_SCORE = 850;
    private static final Integer DEFAULT_AWARD_COUNT = 1;
    private static final String DEFAULT_PROJECT_SUMMARY = "AI 기반 취업 분석 서비스 개발";
    private static final String DEFAULT_PORTFOLIO_LEVEL = "설명 보완 필요";
    private static final List<String> CATEGORIES = List.of(
            "GPA", "Language", "Certifications", "Awards", "ProjectPortfolio");

    private final SpecificationService specificationService;
    private final CompanyRepository companyRepository;
    private final AlumnusRepository alumnusRepository;
    private final UserCertificationRepository userCertificationRepository;
    private final OllamaGapAnalysisClient ollamaGapAnalysisClient;
    private final ObjectMapper objectMapper = new ObjectMapper();

    @Transactional(readOnly = true)
    public GapAnalysisResponse analyzeGap(GapAnalysisRequest request) {
        validateRequest(request);

        Specification specification = specificationService.getMySpecificationEntityForAnalysis();
        Company company = companyRepository.findById(request.getCompanyId())
                .orElseThrow(() -> new IllegalArgumentException("존재하지 않는 기업입니다."));
        Alumnus alumnus = alumnusRepository.findByCompanyIdAndId(request.getCompanyId(), request.getAlumnusId())
                .orElseThrow(() -> new IllegalArgumentException("존재하지 않는 동문입니다."));

        SpecProfileResponse userProfile = createUserProfile(specification);
        SpecProfileResponse alumnusProfile = createAlumnusProfile(alumnus);
        OllamaGapAnalysisResult aiResult = analyzeWithOllama(company, request.getTargetJobRole(),
                userProfile, alumnusProfile);
        aiResult = normalizeResult(aiResult, userProfile, alumnusProfile);

        return GapAnalysisResponse.builder()
                .companyId(company.getId())
                .companyName(company.getName())
                .targetJobRole(hasText(request.getTargetJobRole()) ? request.getTargetJobRole() : alumnus.getJobRole())
                .totalScore(clamp(aiResult.getTotalScore(), 0, 100))
                .scoreDescription(defaultText(aiResult.getScoreDescription(), "AI 분석을 바탕으로 준비도를 산출했습니다."))
                .summary(defaultText(aiResult.getSummary(), "동문 스펙과 비교한 보완 우선순위를 확인해 주세요."))
                .userProfile(userProfile)
                .alumnusProfile(alumnusProfile)
                .comparisonItems(toComparisonItems(aiResult, userProfile, alumnusProfile))
                .detailAnalysis(AiDetailAnalysisResponse.builder()
                        .strengths(emptyToDefault(aiResult.getStrengths(), "현재 보유한 직무 경험을 중심으로 강점을 정리해 주세요."))
                        .weaknesses(emptyToDefault(aiResult.getWeaknesses(), "프로젝트 성과와 설명을 더 구체화해야 합니다."))
                        .comments(emptyToDefault(aiResult.getComments(), "Ollama 응답을 해석하지 못해 기본 안내를 제공합니다."))
                        .build())
                .build();
    }

    private void validateRequest(GapAnalysisRequest request) {
        if (request == null) {
            throw new IllegalArgumentException("분석 요청을 입력해 주세요.");
        }
        if (request.getCompanyId() == null) {
            throw new IllegalArgumentException("기업 ID를 입력해 주세요.");
        }
        if (request.getAlumnusId() == null) {
            throw new IllegalArgumentException("동문 ID를 입력해 주세요.");
        }
    }

    private SpecProfileResponse createUserProfile(Specification specification) {
        int certificationCount = userCertificationRepository.findByUserId(CURRENT_USER_ID).size();
        return SpecProfileResponse.builder()
                .name(DEFAULT_USER_NAME)
                .profileImageUrl("")
                .gpa(specification.getGpa())
                .maxGpa(specification.getMaxGpa())
                .languageType(DEFAULT_LANGUAGE_TYPE)
                .languageScore(DEFAULT_LANGUAGE_SCORE)
                .certificationCount(certificationCount)
                .awardCount(DEFAULT_AWARD_COUNT)
                .projectSummary(DEFAULT_PROJECT_SUMMARY)
                .portfolioLevel(DEFAULT_PORTFOLIO_LEVEL)
                .build();
    }

    private SpecProfileResponse createAlumnusProfile(Alumnus alumnus) {
        return SpecProfileResponse.builder()
                .name(defaultText(alumnus.getName(), "동문"))
                .profileImageUrl(defaultText(alumnus.getProfileImageUrl(), ""))
                .gpa(alumnus.getGpa())
                .maxGpa(alumnus.getMaxGpa() == null ? BigDecimal.valueOf(4.5) : alumnus.getMaxGpa())
                .languageType(defaultText(alumnus.getLanguageType(), "TOEIC"))
                .languageScore(alumnus.getLanguageScore())
                .certificationCount(alumnus.getCertificationCount() == null ? 0 : alumnus.getCertificationCount())
                .awardCount(alumnus.getAwardCount() == null ? 0 : alumnus.getAwardCount())
                .projectSummary(defaultText(alumnus.getProjectSummary(), "프로젝트 설명 없음"))
                .portfolioLevel(defaultText(alumnus.getPortfolioLevel(), "보통"))
                .build();
    }

    private OllamaGapAnalysisResult analyzeWithOllama(Company company, String targetJobRole,
                                                      SpecProfileResponse userProfile,
                                                      SpecProfileResponse alumnusProfile) {
        try {
            return ollamaGapAnalysisClient.analyze(createPrompt(company, targetJobRole, userProfile, alumnusProfile));
        } catch (Exception e) {
            log.warn("Ollama gap analysis failed. companyId={}, targetJobRole={}, reason={}",
                    company.getId(), targetJobRole, e.getMessage(), e);
            return fallbackResult(userProfile, alumnusProfile);
        }
    }

    private String createPrompt(Company company, String targetJobRole, SpecProfileResponse userProfile,
                                SpecProfileResponse alumnusProfile) throws JsonProcessingException {
        Map<String, Object> input = new LinkedHashMap<>();
        input.put("targetCompanyName", company.getName());
        input.put("targetJobRole", hasText(targetJobRole) ? targetJobRole : company.getMainJobRole());
        input.put("userSpec", userProfile);
        input.put("alumnusSpec", alumnusProfile);
        input.put("evaluationCriteria", Map.of(
                "categories", CATEGORIES,
                "projectPortfolioCriteria", List.of(
                        "직무 연관성", "구현 난이도", "기술 스택 적합성", "문제 해결 과정의 구체성",
                        "결과물 완성도", "설명 가능성", "협업/배포/운영 경험 여부"),
                "notice", "프로젝트/포트폴리오 데이터가 텍스트로 충분하지 않으면 과도하게 점수를 높이지 말고 약점에 설명 보완 필요를 포함한다."
        ));

        return """
                너는 취업 스펙 비교 분석 AI다. 반드시 JSON만 반환해라.
                markdown, 설명 문장, 코드블록을 포함하지 마라.
                동문 스펙은 합격 기준 참고 데이터이며 절대적인 정답은 아니다.
                평가 항목은 GPA, Language, Certifications, Awards, ProjectPortfolio로 고정한다.
                각 항목은 0~100점으로 평가하고 totalScore도 0~100점으로 산출한다.
                사용자의 강점은 합격 동문과 비교했을 때 우위이거나 경쟁력 있는 부분으로 작성한다.
                약점은 보완이 필요한 부분으로 작성한다.
                comments는 한국어 3~4문장으로 작성한다.

                입력 JSON:
                %s

                응답 JSON 스키마:
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
                """.formatted(objectMapper.writeValueAsString(input));
    }

    private OllamaGapAnalysisResult normalizeResult(OllamaGapAnalysisResult result, SpecProfileResponse userProfile,
                                                    SpecProfileResponse alumnusProfile) {
        if (result == null) {
            return fallbackResult(userProfile, alumnusProfile);
        }
        if (result.getItems() == null || result.getItems().isEmpty()) {
            result.setItems(fallbackResult(userProfile, alumnusProfile).getItems());
        }
        return result;
    }

    private List<ComparisonItemResponse> toComparisonItems(OllamaGapAnalysisResult result,
                                                           SpecProfileResponse userProfile,
                                                           SpecProfileResponse alumnusProfile) {
        Map<String, OllamaGapAnalysisResult.Item> itemMap = new LinkedHashMap<>();
        result.getItems().forEach(item -> {
            if (hasText(item.getCategory())) {
                itemMap.put(item.getCategory(), item);
            }
        });

        return CATEGORIES.stream()
                .map(category -> toComparisonItem(category, itemMap.get(category), userProfile, alumnusProfile))
                .toList();
    }

    private ComparisonItemResponse toComparisonItem(String category, OllamaGapAnalysisResult.Item item,
                                                    SpecProfileResponse userProfile,
                                                    SpecProfileResponse alumnusProfile) {
        if (item == null) {
            item = fallbackItem(category, userProfile, alumnusProfile);
        }
        return ComparisonItemResponse.builder()
                .category(category)
                .userValue(valueFor(category, userProfile))
                .alumnusValue(valueFor(category, alumnusProfile))
                .gapDescription(defaultText(item.getGapDescription(), "AI 분석 기준으로 비교가 필요합니다."))
                .aiScore(clamp(item.getAiScore() == null ? 50 : item.getAiScore(), 0, 100))
                .status(item.getStatus() == null ? ComparisonStatus.NEEDS_IMPROVEMENT : item.getStatus())
                .build();
    }

    private OllamaGapAnalysisResult fallbackResult(SpecProfileResponse userProfile,
                                                   SpecProfileResponse alumnusProfile) {
        OllamaGapAnalysisResult result = new OllamaGapAnalysisResult();
        result.setTotalScore(60);
        result.setScoreDescription("AI 분석을 완료하지 못해 기본 비교 안내를 제공합니다.");
        result.setSummary("Ollama 연결 또는 응답 해석에 실패했습니다. 동문 스펙과의 차이를 참고해 보완해 주세요.");
        result.setItems(CATEGORIES.stream()
                .map(category -> fallbackItem(category, userProfile, alumnusProfile))
                .toList());
        result.setStrengths(List.of("현재 입력된 스펙을 기준으로 직무 관련 경험을 정리할 수 있습니다."));
        result.setWeaknesses(List.of("AI 상세 판단을 완료하지 못했습니다.", "프로젝트/포트폴리오 설명 보완 필요"));
        result.setComments(List.of(
                "현재는 기본 안내 메시지를 제공합니다.",
                "Ollama 서버와 모델 설정을 확인한 뒤 다시 분석해 주세요.",
                "프로젝트 성과, 사용 기술, 문제 해결 과정을 구체적으로 입력하면 분석 품질이 좋아집니다."
        ));
        return result;
    }

    private OllamaGapAnalysisResult.Item fallbackItem(String category, SpecProfileResponse userProfile,
                                                      SpecProfileResponse alumnusProfile) {
        OllamaGapAnalysisResult.Item item = new OllamaGapAnalysisResult.Item();
        item.setCategory(category);
        item.setUserValue(valueFor(category, userProfile));
        item.setAlumnusValue(valueFor(category, alumnusProfile));
        item.setGapDescription("AI 분석 실패로 정량 차이만 참고해 주세요.");
        item.setAiScore(50);
        item.setStatus(ComparisonStatus.NEEDS_IMPROVEMENT);
        return item;
    }

    private String valueFor(String category, SpecProfileResponse profile) {
        return switch (category) {
            case "GPA" -> "%s/%s".formatted(profile.getGpa(), profile.getMaxGpa());
            case "Language" -> "%s %s".formatted(profile.getLanguageType(), profile.getLanguageScore());
            case "Certifications" -> "%d개".formatted(profile.getCertificationCount());
            case "Awards" -> "%d개".formatted(profile.getAwardCount());
            case "ProjectPortfolio" -> "%s (%s)".formatted(profile.getProjectSummary(), profile.getPortfolioLevel());
            default -> "";
        };
    }

    private List<String> emptyToDefault(List<String> values, String defaultValue) {
        if (values == null || values.isEmpty()) {
            return List.of(defaultValue);
        }
        return values.stream()
                .filter(this::hasText)
                .map(String::trim)
                .toList();
    }

    private String defaultText(String value, String defaultValue) {
        return hasText(value) ? value.trim() : defaultValue;
    }

    private boolean hasText(String value) {
        return value != null && !value.isBlank();
    }

    private int clamp(Integer value, int min, int max) {
        int safeValue = value == null ? min : value;
        return Math.max(min, Math.min(max, safeValue));
    }
}
