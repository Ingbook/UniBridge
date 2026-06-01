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
import com.example.UniBridge.certification.entity.Certification;
import com.example.UniBridge.certification.entity.UserCertification;
import com.example.UniBridge.certification.repository.UserCertificationRepository;
import com.example.UniBridge.company.Company;
import com.example.UniBridge.company.CompanyRepository;
import com.example.UniBridge.specification.entity.Specification;
import com.example.UniBridge.specification.service.SpecificationService;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.math.BigDecimal;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
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
    private static final String DEFAULT_PORTFOLIO_LEVEL = "기본";
    private static final List<String> CATEGORIES = List.of(
            "GPA", "LANGUAGE", "CERTIFICATION", "AWARD", "PROJECT", "PORTFOLIO");
    private static final Map<String, String> DISPLAY_NAMES = Map.of(
            "GPA", "학점",
            "LANGUAGE", "어학성적",
            "CERTIFICATION", "자격증",
            "AWARD", "수상경력",
            "PROJECT", "프로젝트",
            "PORTFOLIO", "포트폴리오"
    );

    private final SpecificationService specificationService;
    private final CompanyRepository companyRepository;
    private final AlumnusRepository alumnusRepository;
    private final UserCertificationRepository userCertificationRepository;
    private final OllamaGapAnalysisClient ollamaGapAnalysisClient;
    private final AnalysisScoreCalculator analysisScoreCalculator;
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
        AnalysisScoreCalculator.AnalysisScoreResult scoreResult = analysisScoreCalculator.calculate(
                userProfile, alumnusProfile);
        OllamaGapAnalysisResult aiResult = analyzeWithOllama(company, request.getTargetJobRole(),
                userProfile, alumnusProfile);
        aiResult = normalizeResult(aiResult, userProfile, alumnusProfile);

        return GapAnalysisResponse.builder()
                .companyId(company.getId())
                .companyName(company.getName())
                .selectedAlumnusId(alumnus.getId())
                .selectedAlumnusName(alumnusProfile.getName())
                .targetJobRole(hasText(request.getTargetJobRole()) ? request.getTargetJobRole() : alumnus.getJobRole())
                .overallScore(scoreResult.overallScore())
                .totalScore(scoreResult.overallScore())
                .scoreDescription(scoreResult.scoreDescription())
                .summarized(scoreResult.summarized())
                .summary(scoreResult.summarized())
                .userProfile(userProfile)
                .selectedAlumnusProfile(alumnusProfile)
                .alumnusProfile(alumnusProfile)
                .gapItems(scoreResult.gapItems())
                .comparisonItems(scoreResult.gapItems())
                .detailAnalysis(AiDetailAnalysisResponse.builder()
                        .strengths(emptyToDefault(aiResult.getStrengths(),
                                "현재 입력된 스펙 중 점수가 높은 항목을 중심으로 강점을 정리해 주세요."))
                        .weaknesses(emptyToDefault(aiResult.getWeaknesses(),
                                "점수가 낮은 Gap 항목부터 구체적으로 보완해 주세요."))
                        .comments(emptyToDefault(aiResult.getComments(), scoreResult.summarized()))
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
        List<String> certificationNames = userCertificationRepository.findByUserId(CURRENT_USER_ID).stream()
                .map(UserCertification::getCertification)
                .filter(Objects::nonNull)
                .map(Certification::getName)
                .filter(this::hasText)
                .map(String::trim)
                .toList();
        certificationNames = analysisScoreCalculator.validCertificationNames(certificationNames);
        String projectSummary = analysisScoreCalculator.normalizeDescription(specification.getProjectSummary());
        String portfolioDescription = analysisScoreCalculator.normalizeDescription(specification.getPortfolioDescription());
        return SpecProfileResponse.builder()
                .name(DEFAULT_USER_NAME)
                .profileImageUrl("")
                .gpa(specification.getGpa())
                .maxGpa(specification.getMaxGpa())
                .languageType(defaultText(specification.getLanguageType(), DEFAULT_LANGUAGE_TYPE))
                .languageScore(specification.getLanguageScore() == null ? DEFAULT_LANGUAGE_SCORE : specification.getLanguageScore())
                .certificationCount(certificationNames.size())
                .certificationNames(certificationNames)
                .awardCount(safeCount(specification.getAwardCount()))
                .projectSummary(projectSummary)
                .portfolioDescription(portfolioDescription)
                .portfolioLevel(DEFAULT_PORTFOLIO_LEVEL)
                .build();
    }

    private SpecProfileResponse createAlumnusProfile(Alumnus alumnus) {
        List<String> certificationNames = analysisScoreCalculator.validCertificationNamesFromText(
                alumnus.getCertificationSummary());
        int certificationCount = alumnus.getCertificationCount() == null
                ? certificationNames.size()
                : Math.min(alumnus.getCertificationCount(), certificationNames.size());
        return SpecProfileResponse.builder()
                .name(defaultText(alumnus.getName(), "동문"))
                .profileImageUrl(defaultText(alumnus.getProfileImageUrl(), ""))
                .gpa(alumnus.getGpa())
                .maxGpa(alumnus.getMaxGpa() == null ? BigDecimal.valueOf(4.5) : alumnus.getMaxGpa())
                .languageType(defaultText(alumnus.getLanguageType(), "TOEIC"))
                .languageScore(alumnus.getLanguageScore())
                .certificationCount(certificationCount)
                .certificationNames(certificationNames)
                .awardCount(alumnus.getAwardCount() == null ? 0 : alumnus.getAwardCount())
                .projectSummary(analysisScoreCalculator.normalizeDescription(alumnus.getProjectSummary()))
                .portfolioDescription(analysisScoreCalculator.normalizeDescription(alumnus.getPortfolioDescription()))
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
        input.put("targetCompany", company.getName());
        input.put("targetJobRole", hasText(targetJobRole) ? targetJobRole : company.getMainJobRole());
        input.put("userProfile", promptProfile(userProfile, false));
        input.put("alumnusProfile", promptProfile(alumnusProfile, true));
        input.put("evaluationCriteria", Map.of(
                "categories", CATEGORIES,
                "certificationCriteria",
                "CERTIFICATION은 단순 개수만 보지 말고 certificationNames, certificationCount, 목표 회사/직무, 동문 데이터와의 비교를 함께 평가한다. IT 서비스/개발/기획 직무에서는 정보처리기사, SQLD, ADsP, AWS, 리눅스마스터, 컴퓨터활용능력 등을 직무 관련성이 높은 자격증으로 판단할 수 있다.",
                "awardCriteria",
                "AWARD는 수상 개수를 기준으로 평가한다. 단, 수상 내역 상세 설명이 없으면 개수 중심으로만 판단한다.",
                "projectPortfolioCriteria", List.of(
                        "실제 설명 내용을 기준으로 판단한다.",
                        "직무 연관성", "구현 난이도", "기술 스택 적합성", "문제 해결 과정의 구체성",
                        "결과물 완성도", "설명 가능성", "배포/운영 경험 여부"),
                "notice", "프로젝트/포트폴리오 설명이 비어 있거나 너무 짧은 경우에만 설명 보완 필요로 판단한다."
        ));

        return """
                너는 취업 스펙 비교 분석 AI다. 반드시 JSON만 반환해라.
                markdown, 설명 문장, 코드블록을 포함하지 마라.
                동문 스펙은 합격 기준 참고 데이터이며 절대적인 정답은 아니다.
                평가 항목 category는 GPA, LANGUAGE, CERTIFICATION, AWARD, PROJECT, PORTFOLIO로 고정한다.
                각 항목은 0~100점으로 평가하되 서버 정량 점수가 최종 기준이다.
                CERTIFICATION은 자격증 이름과 목표 직무 관련성을 반드시 함께 평가한다.
                AWARD는 수상 개수를 기준으로 비교한다.
                PROJECT와 PORTFOLIO는 실제 projectSummary와 portfolioDescription 내용을 기준으로 평가한다.
                사용자의 강점은 합격 동문과 비교했을 때 우위이거나 경쟁력 있는 부분으로 작성한다.
                약점은 보완이 필요한 부분으로 작성한다.
                comments는 한국어 3~4문장으로 작성한다.

                입력 JSON:
                %s

                응답 JSON 스키마:
                {
                  "totalScore": 0,
                  "scoreDescription": "선택 동문 기준 준비도 설명",
                  "summary": "사용자 입력 내용을 기반으로 한 한 문장 요약",
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

    private Map<String, Object> promptProfile(SpecProfileResponse profile, boolean includeName) {
        Map<String, Object> values = new LinkedHashMap<>();
        if (includeName) {
            values.put("name", profile.getName());
        }
        values.put("gpa", valueFor("GPA", profile));
        values.put("languageScore", valueFor("LANGUAGE", profile));
        values.put("certificationCount", safeCount(profile.getCertificationCount()));
        values.put("certificationNames", safeList(profile.getCertificationNames()));
        values.put("awardCount", safeCount(profile.getAwardCount()));
        values.put("projectSummary", analysisScoreCalculator.normalizeDescription(profile.getProjectSummary()));
        values.put("portfolioDescription", analysisScoreCalculator.normalizeDescription(profile.getPortfolioDescription()));
        return values;
    }

    private OllamaGapAnalysisResult normalizeResult(OllamaGapAnalysisResult result, SpecProfileResponse userProfile,
                                                    SpecProfileResponse alumnusProfile) {
        if (result == null) {
            return fallbackResult(userProfile, alumnusProfile);
        }
        if (result.getItems() == null || result.getItems().isEmpty()) {
            result.setItems(fallbackResult(userProfile, alumnusProfile).getItems());
        }
        result.setItems(result.getItems().stream()
                .map(this::normalizeItem)
                .toList());
        return result;
    }

    private OllamaGapAnalysisResult.Item normalizeItem(OllamaGapAnalysisResult.Item item) {
        if (item == null) {
            return null;
        }
        String normalizedCategory = normalizeCategory(item.getCategory());
        if (!hasText(normalizedCategory)) {
            log.warn("Unknown gap analysis category returned from Ollama: {}", item.getCategory());
        }
        item.setCategory(normalizedCategory);
        item.setStatus(normalizeStatus(item.getStatus()));
        return item;
    }

    private List<ComparisonItemResponse> toComparisonItems(OllamaGapAnalysisResult result,
                                                           SpecProfileResponse userProfile,
                                                           SpecProfileResponse alumnusProfile) {
        Map<String, OllamaGapAnalysisResult.Item> itemMap = new LinkedHashMap<>();
        result.getItems().forEach(item -> {
            if (item != null && hasText(item.getCategory())) {
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
            log.warn("Ollama gap analysis item missing. category={}", category);
            item = fallbackItem(category, userProfile, alumnusProfile);
        }
        return ComparisonItemResponse.builder()
                .category(category)
                .displayName(DISPLAY_NAMES.getOrDefault(category, category))
                .userValue(valueFor(category, userProfile))
                .alumnusValue(valueFor(category, alumnusProfile))
                .gapDescription(defaultText(item.getGapDescription(), "AI 분석 기준으로 비교가 필요합니다."))
                .aiScore(clamp(item.getAiScore() == null ? 50 : item.getAiScore(), 0, 100))
                .status(normalizeStatus(item.getStatus()))
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
        result.setWeaknesses(List.of("AI 상세 판단을 완료하지 못했습니다.", "점수가 낮은 Gap 항목부터 보완해 주세요."));
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
        item.setStatus(ComparisonStatus.NEEDS_IMPROVEMENT.name());
        return item;
    }

    private String valueFor(String category, SpecProfileResponse profile) {
        return switch (category) {
            case "GPA" -> "%s/%s".formatted(profile.getGpa(), profile.getMaxGpa());
            case "LANGUAGE" -> "%s %s".formatted(profile.getLanguageType(), profile.getLanguageScore());
            case "CERTIFICATION" -> formatCertificationValue(profile.getCertificationNames(),
                    safeCount(profile.getCertificationCount()));
            case "AWARD" -> "%d개".formatted(safeCount(profile.getAwardCount()));
            case "PROJECT" -> analysisScoreCalculator.normalizeDescription(profile.getProjectSummary());
            case "PORTFOLIO" -> analysisScoreCalculator.normalizeDescription(profile.getPortfolioDescription());
            default -> "";
        };
    }

    private String formatCertificationValue(List<String> names, int count) {
        List<String> safeNames = safeList(names);
        if (safeNames.isEmpty()) {
            return "인정 자격증 0개";
        }

        int displayLimit = 3;
        String displayedNames = String.join(", ", safeNames.stream()
                .limit(displayLimit)
                .toList());
        int remainingCount = Math.max(count - displayLimit, 0);
        if (remainingCount > 0) {
            displayedNames += " 외 %d개".formatted(remainingCount);
        }
        return "%s / 인정 자격증 %d개".formatted(displayedNames, safeNames.size());
    }

    private List<String> parseCertificationNames(String certificationSummary) {
        if (!hasText(certificationSummary)) {
            return List.of();
        }
        return Arrays.stream(certificationSummary.split(","))
                .map(String::trim)
                .filter(this::hasText)
                .toList();
    }

    private String normalizeCategory(String category) {
        if (!hasText(category)) {
            return null;
        }
        return switch (category.trim()) {
            case "GPA" -> "GPA";
            case "LANGUAGE", "Language" -> "LANGUAGE";
            case "CERTIFICATION", "Certifications", "Certification" -> "CERTIFICATION";
            case "AWARD", "Awards", "Award" -> "AWARD";
            case "PROJECT", "Project" -> "PROJECT";
            case "PORTFOLIO", "Portfolio" -> "PORTFOLIO";
            case "PROJECT_PORTFOLIO", "ProjectPortfolio", "Project_Portfolio" -> "PROJECT";
            default -> null;
        };
    }

    private String normalizeStatus(String status) {
        if (!hasText(status)) {
            return ComparisonStatus.NEEDS_IMPROVEMENT.name();
        }
        try {
            return ComparisonStatus.valueOf(status.trim()).name();
        } catch (IllegalArgumentException e) {
            log.warn("Unknown gap analysis status returned from Ollama: {}", status);
            return ComparisonStatus.NEEDS_IMPROVEMENT.name();
        }
    }

    private int safeCount(Integer count) {
        return count == null ? 0 : count;
    }

    private List<String> safeList(List<String> values) {
        if (values == null || values.isEmpty()) {
            return List.of();
        }
        return values.stream()
                .filter(this::hasText)
                .map(String::trim)
                .toList();
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
