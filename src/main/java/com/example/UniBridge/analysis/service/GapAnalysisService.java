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
    private static final String PROJECT_DESCRIPTION_FALLBACK = "설명 보완 필요";
    private static final List<String> CATEGORIES = List.of(
            "GPA", "LANGUAGE", "CERTIFICATION", "AWARD", "PROJECT_PORTFOLIO");
    private static final Map<String, String> DISPLAY_NAMES = Map.of(
            "GPA", "학점",
            "LANGUAGE", "어학성적",
            "CERTIFICATION", "자격증",
            "AWARD", "수상경력",
            "PROJECT_PORTFOLIO", "프로젝트/포트폴리오"
    );

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
        List<String> certificationNames = userCertificationRepository.findByUserId(CURRENT_USER_ID).stream()
                .map(UserCertification::getCertification)
                .filter(Objects::nonNull)
                .map(Certification::getName)
                .filter(this::hasText)
                .map(String::trim)
                .toList();
        String projectSummary = resolveProjectDescription(specification.getProjectSummary());
        String portfolioDescription = resolveProjectDescription(specification.getPortfolioDescription());
        return SpecProfileResponse.builder()
                .name(DEFAULT_USER_NAME)
                .profileImageUrl("")
                .gpa(specification.getGpa())
                .maxGpa(specification.getMaxGpa())
                .languageType(DEFAULT_LANGUAGE_TYPE)
                .languageScore(DEFAULT_LANGUAGE_SCORE)
                .certificationCount(certificationNames.size())
                .certificationNames(certificationNames)
                .awardCount(safeCount(specification.getAwardCount()))
                .projectSummary(projectSummary)
                .portfolioDescription(portfolioDescription)
                .portfolioLevel(DEFAULT_PORTFOLIO_LEVEL)
                .build();
    }

    private SpecProfileResponse createAlumnusProfile(Alumnus alumnus) {
        List<String> certificationNames = parseCertificationNames(alumnus.getCertificationSummary());
        int certificationCount = alumnus.getCertificationCount() == null
                ? certificationNames.size()
                : alumnus.getCertificationCount();
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
                .projectSummary(resolveProjectDescription(alumnus.getProjectSummary()))
                .portfolioDescription(resolveProjectDescription(alumnus.getPortfolioDescription()))
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
                "notice", "프로젝트/포트폴리오 설명이 비어 있는 경우에만 설명 보완 필요로 판단한다."
        ));

        return """
                너는 취업 스펙 비교 분석 AI다. 반드시 JSON만 반환해라.
                markdown, 설명 문장, 코드블록을 포함하지 마라.
                동문 스펙은 합격 기준 참고 데이터이며 절대적인 정답은 아니다.
                평가 항목 category는 GPA, LANGUAGE, CERTIFICATION, AWARD, PROJECT_PORTFOLIO로 고정한다.
                각 항목은 0~100점으로 평가하고 totalScore도 0~100점으로 산출한다.
                CERTIFICATION은 자격증 이름과 목표 직무 관련성을 반드시 함께 평가한다.
                AWARD는 수상 개수를 기준으로 비교한다.
                PROJECT_PORTFOLIO는 실제 projectSummary와 portfolioDescription 내용을 기준으로 평가한다.
                            사용자의 포트폴리오 설명은 100자 내외의 짧은 문장일 수 있다.
                            따라서 긴 자기소개서 수준의 구체성을 요구하지 마라.
                            설명이 짧다는 이유만으로 낮은 점수를 주지 마라.
                            직무 연관성, 핵심 경험, 사용 기술, 역할, 결과 중 일부라도 명확하면 긍정적으로 평가하라.
                            없는 경험, 성과, 기술 스택은 절대 추측하지 마라.

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
        values.put("projectSummary", resolveProjectDescription(profile.getProjectSummary()));
        values.put("portfolioDescription", resolveProjectDescription(profile.getPortfolioDescription()));
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
            case "PROJECT_PORTFOLIO" -> resolveProjectDescription(profile.getProjectSummary());
            default -> "";
        };
    }

    private String formatCertificationValue(List<String> names, int count) {
        List<String> safeNames = safeList(names);
        if (safeNames.isEmpty()) {
            return "총 %d개".formatted(count);
        }

        int displayLimit = 3;
        String displayedNames = String.join(", ", safeNames.stream()
                .limit(displayLimit)
                .toList());
        int remainingCount = Math.max(count - displayLimit, 0);
        if (remainingCount > 0) {
            displayedNames += " 외 %d개".formatted(remainingCount);
        }
        return "%s / 총 %d개".formatted(displayedNames, count);
    }

    private String resolveProjectDescription(String description) {
        return hasText(description) ? description.trim() : PROJECT_DESCRIPTION_FALLBACK;
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
            case "PROJECT_PORTFOLIO", "ProjectPortfolio", "Project_Portfolio" -> "PROJECT_PORTFOLIO";
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
