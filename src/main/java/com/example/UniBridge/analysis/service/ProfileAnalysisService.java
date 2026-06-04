package com.example.UniBridge.analysis.service;

import com.example.UniBridge.analysis.dto.AiProfileAnalysisRequest;
import com.example.UniBridge.analysis.dto.AiProfileAnalysisResponse;
import com.example.UniBridge.certification.entity.Certification;
import com.example.UniBridge.certification.entity.UserCertification;
import com.example.UniBridge.certification.repository.CertificationRepository;
import com.example.UniBridge.certification.repository.UserCertificationRepository;
import com.example.UniBridge.specification.entity.Specification;
import com.example.UniBridge.specification.repository.SpecificationRepository;
import java.math.BigDecimal;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class ProfileAnalysisService {

    private static final Long CURRENT_USER_ID = 1L;
    private static final BigDecimal MAX_GPA = BigDecimal.valueOf(4.5);
    private static final String DEFAULT_USER_NAME = "현재 사용자";
    private static final String INVALID_CERTIFICATION_MESSAGE =
            "입력 가능한 자격증은 정보처리기사, SQLD, ADsP, AWS Cloud Practitioner, 리눅스마스터 2급, 컴퓨터활용능력 1급입니다.";

    private static final Set<String> ALLOWED_CERTIFICATIONS = Set.of(
            "정보처리기사",
            "SQLD",
            "ADsP",
            "AWS Cloud Practitioner",
            "리눅스마스터 2급",
            "컴퓨터활용능력 1급"
    );

    private final LocalAiAnalysisService localAiAnalysisService;
    private final SpecificationRepository specificationRepository;
    private final CertificationRepository certificationRepository;
    private final UserCertificationRepository userCertificationRepository;

    @Transactional
    public AiProfileAnalysisResponse analyzeProfile(AiProfileAnalysisRequest request) {
        if (request == null) {
            throw new IllegalArgumentException("사용자 프로필을 입력해 주세요.");
        }

        List<String> certifications = normalizeCertifications(request.getCertifications());
        Integer awardCount = normalizeAwardCount(request.getAwardCount());
        String languageType = normalizeText(request.getLanguageType());
        String project = normalizeText(request.getProject());
        String portfolio = normalizeText(request.getPortfolio());

        validateGpa(request.getGpa());
        validateLanguage(languageType, request.getLanguageScore());
        validateAwardCount(awardCount);
        validateCertifications(certifications);

        saveSpecification(request.getGpa(), languageType, request.getLanguageScore(), awardCount, project, portfolio);
        saveUserCertifications(certifications);

        AiProfileAnalysisResponse.UserProfile userProfile =
                createUserProfile(request.getGpa(), languageType, request.getLanguageScore(), certifications,
                        awardCount, project, portfolio);
        AiProfileAnalysisResponse.AiAnalysis analysis;
        try {
            analysis = localAiAnalysisService.analyzeProfile(userProfile);
        } catch (Exception e) {
            analysis = fallbackAnalysis(userProfile);
        }

        return AiProfileAnalysisResponse.builder()
                .userProfile(userProfile)
                .aiAnalysis(analysis)
                .build();
    }

    private void saveSpecification(BigDecimal gpa, String languageType, Integer languageScore,
                                   Integer awardCount, String project, String portfolio) {
        Specification specification = specificationRepository.findByUserId(CURRENT_USER_ID)
                .orElseGet(() -> Specification.builder().userId(CURRENT_USER_ID).build());
        specification.update(gpa, MAX_GPA, languageType, languageScore, awardCount, project, portfolio);
        specificationRepository.save(specification);
    }

    private void saveUserCertifications(List<String> certifications) {
        userCertificationRepository.deleteByUserId(CURRENT_USER_ID);
        certifications.stream()
                .map(this::findCertification)
                .map(certification -> UserCertification.builder()
                        .userId(CURRENT_USER_ID)
                        .certification(certification)
                        .build())
                .forEach(userCertificationRepository::save);
    }

    private Certification findCertification(String name) {
        return certificationRepository.findByName(name)
                .orElseThrow(() -> new IllegalArgumentException("존재하지 않는 자격증입니다: " + name));
    }

    private AiProfileAnalysisResponse.UserProfile createUserProfile(
            BigDecimal gpa,
            String languageType,
            Integer languageScore,
            List<String> certifications,
            Integer awardCount,
            String project,
            String portfolio
    ) {
        return AiProfileAnalysisResponse.UserProfile.builder()
                .name(DEFAULT_USER_NAME)
                .gpa(gpa)
                .language(AiProfileAnalysisResponse.Language.builder()
                        .type(languageType)
                        .score(languageScore)
                        .displayText("%s %d".formatted(languageType, languageScore))
                        .build())
                .certifications(AiProfileAnalysisResponse.Certifications.builder()
                        .items(certifications)
                        .count(certifications.size())
                        .build())
                .awardCount(awardCount)
                .project(project)
                .portfolio(portfolio)
                .build();
    }

    private List<String> normalizeCertifications(List<String> certifications) {
        if (certifications == null || certifications.isEmpty()) {
            return List.of();
        }

        return certifications.stream()
                .filter(this::hasText)
                .map(String::trim)
                .collect(Collectors.toCollection(LinkedHashSet::new))
                .stream()
                .toList();
    }

    private Integer normalizeAwardCount(Integer awardCount) {
        return awardCount == null ? 0 : awardCount;
    }

    private String normalizeText(String value) {
        return hasText(value) ? value.trim() : "";
    }

    private void validateGpa(BigDecimal gpa) {
        if (gpa == null) {
            throw new IllegalArgumentException("학점을 입력해 주세요.");
        }
        if (gpa.signum() < 0 || gpa.compareTo(MAX_GPA) > 0) {
            throw new IllegalArgumentException("학점은 0.0 이상 4.5 이하로 입력해 주세요.");
        }
    }

    private void validateLanguage(String languageType, Integer languageScore) {
        if (!hasText(languageType)) {
            throw new IllegalArgumentException("어학 시험 종류를 입력해 주세요.");
        }
        if (languageScore == null || languageScore <= 0) {
            throw new IllegalArgumentException("어학 점수는 0보다 큰 숫자로 입력해 주세요.");
        }
    }

    private void validateAwardCount(Integer awardCount) {
        if (awardCount != null && awardCount < 0) {
            throw new IllegalArgumentException("수상경력 개수는 0 이상이어야 합니다.");
        }
    }

    private void validateCertifications(List<String> certifications) {
        if (certifications == null || certifications.isEmpty()) {
            return;
        }
        boolean hasInvalidCertification = certifications.stream()
                .anyMatch(certification -> !ALLOWED_CERTIFICATIONS.contains(certification));
        if (hasInvalidCertification) {
            throw new IllegalArgumentException(INVALID_CERTIFICATION_MESSAGE);
        }
    }

    private AiProfileAnalysisResponse.AiAnalysis fallbackAnalysis(AiProfileAnalysisResponse.UserProfile userProfile) {
        boolean hasProject = hasText(userProfile.getProject());
        boolean hasPortfolio = hasText(userProfile.getPortfolio());
        return AiProfileAnalysisResponse.AiAnalysis.builder()
                .strengths(List.of(resolveStrength(userProfile)))
                .weaknesses(List.of(resolveWeakness(hasProject, hasPortfolio)))
                .comment(resolveComment(hasProject, hasPortfolio))
                .build();
    }

    private String resolveStrength(AiProfileAnalysisResponse.UserProfile userProfile) {
        if (hasText(userProfile.getPortfolio())) {
            return "포트폴리오를 통해 프로젝트 경험과 직무 역량을 함께 설명할 수 있습니다.";
        }
        if (userProfile.getGpa().compareTo(BigDecimal.valueOf(3.5)) >= 0) {
            return "학점이 준수합니다.";
        }
        if (!userProfile.getCertifications().getItems().isEmpty()) {
            return String.join(", ", userProfile.getCertifications().getItems())
                    + " 보유로 기본 개발 역량을 확인할 수 있습니다.";
        }
        return "입력된 프로필을 기준으로 취업 준비 상태를 점검할 수 있습니다.";
    }

    private String resolveWeakness(boolean hasProject, boolean hasPortfolio) {
        if (!hasProject) {
            return "프로젝트 설명 보완 필요: 사용 기술, 담당 역할, 성과를 입력해 주세요.";
        }
        if (!hasPortfolio) {
            return "포트폴리오 설명 보완 필요: 프로젝트 결과물, 링크, 구성, 본인 기여도를 입력해 주세요.";
        }
        return "프로젝트와 포트폴리오 설명에 사용 기술, 담당 역할, 성과를 더 구체적으로 작성하면 좋습니다.";
    }

    private String resolveComment(boolean hasProject, boolean hasPortfolio) {
        if (!hasProject) {
            return "프로젝트 설명 보완 필요: 프로젝트에서 사용 기술, 담당 역할, 성과를 추가하면 더 좋은 평가를 받을 수 있습니다.";
        }
        if (!hasPortfolio) {
            return "포트폴리오 설명 보완 필요: 프로젝트 결과물, 배포 링크, 본인 기여도를 추가하면 더 좋은 평가를 받을 수 있습니다.";
        }
        return "현재 프로필은 백엔드/데이터 직무 지원에 활용하기 좋은 구성을 가지고 있습니다. 프로젝트와 포트폴리오에서 사용 기술, 담당 역할, 성과를 구체적으로 보완하면 더 좋은 평가를 받을 수 있습니다.";
    }

    private boolean hasText(String value) {
        return value != null && !value.isBlank();
    }
}
