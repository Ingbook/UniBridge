package com.example.UniBridge.analysis;

import com.example.UniBridge.company.Company;
import com.example.UniBridge.company.CompanyService;
import com.example.UniBridge.specification.Specification;
import com.example.UniBridge.specification.SpecificationService;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

@Service
@RequiredArgsConstructor
public class AnalysisService {

    private static final Long CURRENT_USER_ID = 1L;

    private final AnalysisReportRepository analysisReportRepository;
    private final CompanyService companyService;
    private final SpecificationService specificationService;

    @Transactional
    public AnalysisReportDto analyzeGap(GapAnalysisRequest request) {
        if (request.getCompanyId() == null) {
            throw new IllegalArgumentException("기업 ID를 입력해 주세요.");
        }

        Company company = companyService.getCompanyEntity(request.getCompanyId());
        Specification specification = specificationService.getMySpecificationForAnalysis();

        int gpaScore = calculateGpaScore(specification);
        int languageScore = calculateLanguageScore(specification.getLanguageScore());
        int certificationScore = calculateCertificationScore(specification.getCertifications());
        int projectScore = calculateProjectScore(specification.getProjects());
        int activityScore = calculateActivityScore(specification.getAwards(), specification.getInternships());
        int totalScore = (int) Math.round(
                gpaScore * 0.25
                        + languageScore * 0.20
                        + certificationScore * 0.20
                        + projectScore * 0.25
                        + activityScore * 0.10
        );
        int gapScore = company.getAverageScore() - totalScore;

        AnalysisReport report = AnalysisReport.builder()
                .userId(CURRENT_USER_ID)
                .company(company)
                .companyName(company.getName())
                .mainJobRole(company.getMainJobRole())
                .userScore(totalScore)
                .targetAverageScore(company.getAverageScore())
                .gapScore(gapScore)
                .gpaScore(gpaScore)
                .languageScore(languageScore)
                .certificationScore(certificationScore)
                .projectScore(projectScore)
                .activityScore(activityScore)
                .strengths(buildStrengths(gpaScore, languageScore, certificationScore, projectScore, activityScore, gapScore))
                .weaknesses(buildWeaknesses(gpaScore, languageScore, certificationScore, projectScore, activityScore))
                .recommendations(buildRecommendations(gpaScore, languageScore, certificationScore, projectScore, activityScore, gapScore))
                .build();

        return AnalysisReportDto.from(analysisReportRepository.save(report));
    }

    @Transactional(readOnly = true)
    public List<AnalysisReportDto> getMyReports() {
        return analysisReportRepository.findByUserIdOrderByIdDesc(CURRENT_USER_ID).stream()
                .map(AnalysisReportDto::from)
                .toList();
    }

    @Transactional(readOnly = true)
    public AnalysisReportDto getReport(Long analysisId) {
        return AnalysisReportDto.from(analysisReportRepository.findById(analysisId)
                .orElseThrow(() -> new IllegalArgumentException("존재하지 않는 분석 결과입니다.")));
    }

    private int calculateGpaScore(Specification specification) {
        if (specification.getGpa() == null || specification.getMaxGpa() == null || specification.getMaxGpa().signum() <= 0) {
            return 0;
        }
        return clamp(specification.getGpa()
                .divide(specification.getMaxGpa(), 4, RoundingMode.HALF_UP)
                .multiply(BigDecimal.valueOf(100))
                .setScale(0, RoundingMode.HALF_UP)
                .intValue());
    }

    private int calculateLanguageScore(Integer rawScore) {
        if (rawScore == null || rawScore <= 0) {
            return 0;
        }
        return clamp((int) Math.round(rawScore / 990.0 * 100));
    }

    private int calculateCertificationScore(String certifications) {
        if (!StringUtils.hasText(certifications)) {
            return 0;
        }
        String[] items = certifications.split("[,\\n]");
        int count = 0;
        for (String item : items) {
            if (StringUtils.hasText(item)) {
                count++;
            }
        }
        return clamp(count * 20);
    }

    private int calculateProjectScore(String projects) {
        if (!StringUtils.hasText(projects)) {
            return 40;
        }
        int score = 75;
        String lowerProjects = projects.toLowerCase();
        List<String> keywords = List.of("spring", "java", "jpa", "db", "api", "배포");
        for (String keyword : keywords) {
            if (lowerProjects.contains(keyword.toLowerCase())) {
                score += 5;
            }
        }
        return clamp(score);
    }

    private int calculateActivityScore(String awards, String internships) {
        return StringUtils.hasText(awards) || StringUtils.hasText(internships) ? 80 : 50;
    }

    private String buildStrengths(int gpaScore, int languageScore, int certificationScore, int projectScore, int activityScore, int gapScore) {
        StringBuilder builder = new StringBuilder();
        appendIfStrong(builder, gpaScore, "학점");
        appendIfStrong(builder, languageScore, "어학");
        appendIfStrong(builder, certificationScore, "자격증");
        appendIfStrong(builder, projectScore, "프로젝트");
        appendIfStrong(builder, activityScore, "대외활동 및 실무경험");
        if (gapScore <= 0) {
            append(builder, "목표 기업 평균보다 높은 종합 경쟁력을 보유하고 있습니다.");
        }
        return builder.length() == 0 ? "뚜렷한 강점이 아직 부족합니다." : builder.toString();
    }

    private String buildWeaknesses(int gpaScore, int languageScore, int certificationScore, int projectScore, int activityScore) {
        StringBuilder builder = new StringBuilder();
        appendIfWeak(builder, gpaScore, "학점");
        appendIfWeak(builder, languageScore, "어학");
        appendIfWeak(builder, certificationScore, "자격증");
        appendIfWeak(builder, projectScore, "프로젝트");
        appendIfWeak(builder, activityScore, "대외활동 및 실무경험");
        return builder.length() == 0 ? "목표 기업 기준에서 큰 약점은 확인되지 않았습니다." : builder.toString();
    }

    private String buildRecommendations(int gpaScore, int languageScore, int certificationScore, int projectScore, int activityScore, int gapScore) {
        StringBuilder builder = new StringBuilder();
        if (gpaScore < 70) {
            append(builder, "전공 핵심 과목 성적을 보완하고 학점 회복 계획을 세우세요.");
        }
        if (languageScore < 70) {
            append(builder, "TOEIC 등 공인 어학 점수를 목표 직무 기준에 맞춰 개선하세요.");
        }
        if (certificationScore < 60) {
            append(builder, "직무와 연결되는 자격증을 1개 이상 추가 취득하세요.");
        }
        if (projectScore < 80) {
            append(builder, "Spring Boot, Java, JPA, DB, API, 배포 경험이 드러나는 프로젝트를 보강하세요.");
        }
        if (activityScore < 70) {
            append(builder, "인턴십, 공모전, 수상 경험 등 실무형 활동을 추가하세요.");
        }
        if (builder.length() == 0 && gapScore <= 0) {
            append(builder, "현재 수준을 유지하면서 포트폴리오 설명과 면접 답변 완성도를 높이세요.");
        }
        return builder.toString();
    }

    private void appendIfStrong(StringBuilder builder, int score, String label) {
        if (score >= 80) {
            append(builder, label + " 역량이 우수합니다.");
        }
    }

    private void appendIfWeak(StringBuilder builder, int score, String label) {
        if (score < 60) {
            append(builder, label + " 역량 보완이 필요합니다.");
        }
    }

    private void append(StringBuilder builder, String text) {
        if (builder.length() > 0) {
            builder.append(" ");
        }
        builder.append(text);
    }

    private int clamp(int value) {
        return Math.max(0, Math.min(100, value));
    }
}
