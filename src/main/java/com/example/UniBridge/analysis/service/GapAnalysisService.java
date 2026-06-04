package com.example.UniBridge.analysis.service;

import com.example.UniBridge.alumnus.Alumnus;
import com.example.UniBridge.alumnus.AlumnusRepository;
import com.example.UniBridge.analysis.dto.AnalysisProfileResponse;
import com.example.UniBridge.analysis.dto.CertificationValue;
import com.example.UniBridge.analysis.dto.ComparisonItemResponse;
import com.example.UniBridge.analysis.dto.ComparisonStatus;
import com.example.UniBridge.analysis.dto.FieldComment;
import com.example.UniBridge.analysis.dto.FieldCommentsResponse;
import com.example.UniBridge.analysis.dto.GapItemResponse;
import com.example.UniBridge.analysis.dto.GapAnalysisRequest;
import com.example.UniBridge.analysis.dto.GapAnalysisResponse;
import com.example.UniBridge.analysis.dto.LanguageValue;
import com.example.UniBridge.analysis.dto.OllamaGapAnalysisResult;
import com.example.UniBridge.analysis.dto.OverallCommentResponse;
import com.example.UniBridge.analysis.dto.SpecProfileResponse;
import com.example.UniBridge.analysis.ollama.OllamaGapAnalysisClient;
import com.example.UniBridge.certification.entity.Certification;
import com.example.UniBridge.certification.entity.UserCertification;
import com.example.UniBridge.certification.repository.UserCertificationRepository;
import com.example.UniBridge.company.Company;
import com.example.UniBridge.company.CompanyRepository;
import com.example.UniBridge.specification.entity.Specification;
import com.example.UniBridge.specification.repository.SpecificationRepository;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.math.BigDecimal;
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

    private final SpecificationRepository specificationRepository;
    private final CompanyRepository companyRepository;
    private final AlumnusRepository alumnusRepository;
    private final UserCertificationRepository userCertificationRepository;
    private final OllamaGapAnalysisClient ollamaGapAnalysisClient;
    private final AnalysisScoreCalculator analysisScoreCalculator;
    private final ObjectMapper objectMapper = new ObjectMapper();

    @Transactional(readOnly = true)
    public GapAnalysisResponse analyzeGap(GapAnalysisRequest request) {
        validateRequest(request);

        Specification specification = specificationRepository.findByUserId(CURRENT_USER_ID).orElse(new Specification(CURRENT_USER_ID));
        Company company = companyRepository.findById(request.getCompanyId())
                .orElseThrow(() -> new IllegalArgumentException("존재하지 않는 기업입니다."));
        Alumnus alumnus = alumnusRepository.findByCompanyIdAndId(request.getCompanyId(), request.getAlumnusId())
                .orElseThrow(() -> new IllegalArgumentException("존재하지 않는 동문입니다."));

        SpecProfileResponse userProfile = createUserProfile(specification);
        SpecProfileResponse alumnusProfile = createAlumnusProfile(alumnus);
        AnalysisScoreCalculator.AnalysisScoreResult scoreResult = analysisScoreCalculator.calculate(
                userProfile, alumnusProfile);
        OllamaGapAnalysisResult aiResult = analyzeWithOllama(company, request.getTargetJobRole(),
                userProfile, alumnusProfile, scoreResult);
        aiResult = normalizeResult(aiResult, userProfile, alumnusProfile);
        List<GapItemResponse> gapItems = createGapItems(scoreResult, aiResult, userProfile, alumnusProfile);
        FieldCommentsResponse fieldComments = createFieldComments(gapItems);

        return GapAnalysisResponse.builder()
                .currentUser(toProfileResponse(userProfile))
                .selectedAlumnus(toProfileResponse(alumnusProfile))
                .gapItems(gapItems)
                .fieldComments(fieldComments)
                .overallComment(createOverallComment(aiResult, scoreResult))
                .companyId(company.getId())
                .companyName(company.getName())
                .selectedAlumnusId(alumnus.getId())
                .targetJobRole(hasText(request.getTargetJobRole()) ? request.getTargetJobRole() : alumnus.getJobRole())
                .overallScore(scoreResult.overallScore())
                .scoreDescription(defaultText(aiResult.getScoreDescription(), scoreResult.scoreDescription()))
                .summary(defaultText(aiResult.getSummary(), scoreResult.summarized()))
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
        List<String> requestedCertificationNames = userCertificationRepository.findByUserId(CURRENT_USER_ID).stream()
                .map(UserCertification::getCertification)
                .filter(Objects::nonNull)
                .map(Certification::getName)
                .filter(this::hasText)
                .map(String::trim)
                .toList();
        validateAllowedCertifications(requestedCertificationNames);
        List<String> certificationNames = analysisScoreCalculator.validCertificationNames(requestedCertificationNames);
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
                                                      SpecProfileResponse alumnusProfile,
                                                      AnalysisScoreCalculator.AnalysisScoreResult scoreResult) {
        try {
            return ollamaGapAnalysisClient.analyze(createPrompt(company, targetJobRole, userProfile, alumnusProfile));
        } catch (Exception e) {
            log.warn("Ollama gap analysis failed. companyId={}, targetJobRole={}, reason={}",
                    company.getId(), targetJobRole, e.getMessage(), e);
            return fallbackResult(userProfile, alumnusProfile, scoreResult);
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

    private AnalysisProfileResponse toProfileResponse(SpecProfileResponse profile) {
        return AnalysisProfileResponse.builder()
                .name(defaultText(profile.getName(), ""))
                .gpa(profile.getGpa())
                .language(languageValue(profile))
                .certifications(certificationValue(profile))
                .awardCount(safeCount(profile.getAwardCount()))
                .project(analysisScoreCalculator.normalizeDescription(profile.getProjectSummary()))
                .portfolio(analysisScoreCalculator.normalizeDescription(profile.getPortfolioDescription()))
                .build();
    }

    private List<GapItemResponse> createGapItems(AnalysisScoreCalculator.AnalysisScoreResult scoreResult,
                                                 OllamaGapAnalysisResult aiResult,
                                                 SpecProfileResponse userProfile,
                                                 SpecProfileResponse alumnusProfile) {
        Map<String, ComparisonItemResponse> scoreItems = new LinkedHashMap<>();
        scoreResult.gapItems().forEach(item -> scoreItems.put(item.getCategory(), item));

        Map<String, OllamaGapAnalysisResult.Item> aiItems = new LinkedHashMap<>();
        aiResult.getItems().forEach(item -> {
            if (item != null && hasText(item.getCategory())) {
                aiItems.put(item.getCategory(), item);
            }
        });

        return CATEGORIES.stream()
                .map(category -> createGapItem(category, scoreItems.get(category), aiItems.get(category),
                        userProfile, alumnusProfile))
                .toList();
    }

    private GapItemResponse createGapItem(String category, ComparisonItemResponse scoreItem,
                                          OllamaGapAnalysisResult.Item aiItem,
                                          SpecProfileResponse userProfile,
                                          SpecProfileResponse alumnusProfile) {
        if (scoreItem == null) {
            scoreItem = toComparisonItem(category, aiItem, userProfile, alumnusProfile);
        }
        String currentValue = valueFor(category, userProfile);
        String alumnusValue = valueFor(category, alumnusProfile);
        String message = aiItem == null ? null : aiItem.getGapDescription();
        if (!hasText(message)) {
            message = scoreItem.getComment();
        }
        String comment = defaultText(fieldCommentFor(category, message, userProfile),
                "%s 항목은 선택 동문 기준으로 추가 확인이 필요합니다.".formatted(DISPLAY_NAMES.getOrDefault(category, category)));
        return GapItemResponse.builder()
                .field(fieldName(category))
                .label(DISPLAY_NAMES.getOrDefault(category, category))
                .currentValue(currentValue)
                .alumnusValue(alumnusValue)
                .displayText("%s → %s".formatted(currentValue, alumnusValue))
                .score(aiItem == null || aiItem.getAiScore() == null
                        ? scoreItem.getScore()
                        : clamp(aiItem.getAiScore()))
                .status(aiItem == null ? scoreItem.getStatus() : normalizeStatus(aiItem.getStatus()))
                .message(defaultText(message, "%s 항목은 선택 동문 기준으로 비교가 필요합니다."
                        .formatted(DISPLAY_NAMES.getOrDefault(category, category))))
                .comment(comment)
                .build();
    }

    private FieldCommentsResponse createFieldComments(List<GapItemResponse> gapItems) {
        Map<String, GapItemResponse> itemMap = new LinkedHashMap<>();
        gapItems.forEach(item -> itemMap.put(item.getField(), item));
        return FieldCommentsResponse.builder()
                .name(FieldComment.builder()
                        .label("이름")
                        .message("현재 사용자 프로필이 정상적으로 입력되었습니다.")
                        .comment("이름은 비교 점수 산정 대상은 아니지만, 분석 결과에서 현재 사용자와 선택 동문을 구분하기 위해 사용됩니다.")
                        .build())
                .gpa(toFieldComment(itemMap.get("gpa")))
                .language(toFieldComment(itemMap.get("language")))
                .certifications(toFieldComment(itemMap.get("certifications")))
                .awardCount(toFieldComment(itemMap.get("awardCount")))
                .project(toFieldComment(itemMap.get("project")))
                .portfolio(toFieldComment(itemMap.get("portfolio")))
                .build();
    }

    private FieldComment toFieldComment(GapItemResponse item) {
        return FieldComment.builder()
                .label(item.getLabel())
                .message(defaultText(item.getMessage(), "%s 항목은 선택 동문 기준으로 비교가 필요합니다.".formatted(item.getLabel())))
                .comment(defaultText(item.getComment(), "%s 항목의 보완 방향을 구체적으로 정리해 주세요.".formatted(item.getLabel())))
                .build();
    }

    private OverallCommentResponse createOverallComment(OllamaGapAnalysisResult aiResult,
                                                        AnalysisScoreCalculator.AnalysisScoreResult scoreResult) {
        List<String> comments = emptyToDefault(aiResult.getComments(), scoreResult.summarized());
        return OverallCommentResponse.builder()
                .strengths(emptyToDefault(aiResult.getStrengths(),
                        "현재 입력된 스펙 중 점수가 높은 항목을 중심으로 강점을 정리해 주세요."))
                .weaknesses(emptyToDefault(aiResult.getWeaknesses(),
                        "점수가 낮은 Gap 항목부터 구체적으로 보완해 주세요."))
                .aiComment(String.join(" ", comments))
                .build();
    }

    private LanguageValue languageValue(SpecProfileResponse profile) {
        String type = defaultText(profile.getLanguageType(), DEFAULT_LANGUAGE_TYPE);
        Integer score = profile.getLanguageScore();
        return LanguageValue.builder()
                .type(type)
                .score(score)
                .build();
    }

    private CertificationValue certificationValue(SpecProfileResponse profile) {
        List<String> names = safeList(profile.getCertificationNames());
        return CertificationValue.builder()
                .items(names)
                .count(names.size())
                .build();
    }

    private void validateAllowedCertifications(List<String> certificationNames) {
        List<String> invalidNames = certificationNames.stream()
                .filter(name -> analysisScoreCalculator.validCertificationNames(List.of(name)).isEmpty())
                .toList();
        if (!invalidNames.isEmpty()) {
            throw new IllegalArgumentException("입력 가능한 자격증은 정보처리기사, SQLD, ADsP, AWS Cloud Practitioner, 리눅스마스터 2급, 컴퓨터활용능력 1급입니다.");
        }
    }

    private String fieldName(String category) {
        return switch (category) {
            case "GPA" -> "gpa";
            case "LANGUAGE" -> "language";
            case "CERTIFICATION" -> "certifications";
            case "AWARD" -> "awardCount";
            case "PROJECT" -> "project";
            case "PORTFOLIO" -> "portfolio";
            default -> category.toLowerCase();
        };
    }

    private String fieldCommentFor(String category, String message, SpecProfileResponse userProfile) {
        return switch (category) {
            case "GPA" -> "학점은 기본 역량을 보여주는 지표입니다. 현재 학점도 활용 가능하지만, 선택 동문과 비교해 보완 방향을 확인해 주세요.";
            case "LANGUAGE" -> "어학성적은 글로벌 업무 가능성과 기본 성실성을 보여줄 수 있습니다. 목표 기업 기준에 맞춰 점수 향상 여부를 점검해 주세요.";
            case "CERTIFICATION" -> certificationComment(userProfile);
            case "AWARD" -> "수상경력은 프로젝트 결과물의 객관적인 성과를 보여주는 요소입니다. 공모전, 해커톤, 교내 경진대회 참여를 통해 보완할 수 있습니다.";
            case "PROJECT" -> "현재 프로젝트 주제는 좋지만, 단순 개발 경험보다 배포 여부, 사용자 입력 처리, 데이터 분석 방식, AI 분석 로직 등을 구체적으로 설명하는 가장 강력한 무기입니다.";
            case "PORTFOLIO" -> "포트폴리오에는 프로젝트 개요, 사용 기술, 담당 역할, 문제 해결 과정, GitHub 링크, 배포 링크, 결과 화면을 포함하는 것이 좋습니다.";
            default -> message;
        };
    }

    private String certificationComment(SpecProfileResponse userProfile) {
        List<String> names = safeList(userProfile.getCertificationNames());
        if (names.isEmpty()) {
            return "인정 자격증이 없으면 직무 기초 역량을 보여줄 근거가 부족할 수 있습니다. 목표 직무와 관련된 자격증을 보완해 주세요.";
        }
        return "%s 보유는 개발 직무에서 기본 전공 역량을 보여줄 수 있는 강점입니다. 현재 자격증 항목은 경쟁력으로 활용할 수 있습니다."
                .formatted(String.join(", ", names));
    }

    private OllamaGapAnalysisResult normalizeResult(OllamaGapAnalysisResult result, SpecProfileResponse userProfile,
                                                    SpecProfileResponse alumnusProfile) {
        // We removed the fallback call here since it was moved to analyzeWithOllama
        if (result == null) {
             log.error("AI Analysis result is null after processing");
             return createMinimalResult();
        }
        if (result.getItems() == null || result.getItems().isEmpty()) {
             log.warn("AI Analysis items are empty");
        } else {
            result.setItems(result.getItems().stream()
                .map(this::normalizeItem)
                .toList());
        }
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
                .aiScore(clamp(item.getAiScore() == null ? 50 : item.getAiScore()))
                .status(normalizeStatus(item.getStatus()))
                .build();
    }
    
    private OllamaGapAnalysisResult createMinimalResult() {
         OllamaGapAnalysisResult result = new OllamaGapAnalysisResult();
         result.setScoreDescription("AI 분석 모델 응답 오류");
         result.setSummary("AI가 응답을 생성하지 못했습니다. 서버 관리자에게 문의하세요.");
         return result;
    }

    private OllamaGapAnalysisResult fallbackResult(SpecProfileResponse userProfile,
                                                   SpecProfileResponse alumnusProfile,
                                                   AnalysisScoreCalculator.AnalysisScoreResult scoreResult) {
        OllamaGapAnalysisResult result = new OllamaGapAnalysisResult();
        result.setTotalScore(scoreResult.overallScore());
        
        // Pass the actual calculated descriptions instead of the hardcoded AI prompt text
        result.setScoreDescription(scoreResult.scoreDescription());
        result.setSummary("AI 서버 오류로 정량 분석 결과만 제공됩니다: " + scoreResult.summarized());
        
        result.setItems(CATEGORIES.stream()
                .map(category -> fallbackItem(category, userProfile, alumnusProfile))
                .toList());
        result.setStrengths(List.of("서버 기반 정량 점수 산출 완료."));
        result.setWeaknesses(List.of("AI 모델 서버 타임아웃 또는 응답 오류.", "상세 분석 불가."));
        result.setComments(List.of(
                "Ollama 서버가 연결되지 않거나 올바른 JSON을 반환하지 않았습니다.",
                "현재 제공된 점수는 자체 서버 알고리즘에 기반한 정량 분석 점수입니다."
        ));
        return result;
    }

    private OllamaGapAnalysisResult.Item fallbackItem(String category, SpecProfileResponse userProfile,
                                                      SpecProfileResponse alumnusProfile) {
        OllamaGapAnalysisResult.Item item = new OllamaGapAnalysisResult.Item();
        item.setCategory(category);
        item.setUserValue(valueFor(category, userProfile));
        item.setAlumnusValue(valueFor(category, alumnusProfile));
        item.setGapDescription("AI 상세 비교 실패. 정량 수치를 참고하세요.");
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

    private String normalizeCategory(String category) {
        if (!hasText(category)) {
            return null;
        }
        return switch (category.trim()) {
            case "GPA" -> "GPA";
            case "LANGUAGE", "Language" -> "LANGUAGE";
            case "CERTIFICATION", "Certifications", "Certification" -> "CERTIFICATION";
            case "AWARD", "Awards", "Award" -> "AWARD";
            case "PROJECT", "Project", "PROJECT_PORTFOLIO", "ProjectPortfolio", "Project_Portfolio" -> "PROJECT";
            case "PORTFOLIO", "Portfolio" -> "PORTFOLIO";
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

    private int clamp(Integer value) {
        int safeValue = value == null ? 0 : value;
        return Math.max(0, Math.min(100, safeValue));
    }
}