package com.example.UniBridge.analysis.service;

import com.example.UniBridge.analysis.dto.AiAnalysisResult;
import com.example.UniBridge.analysis.dto.AnalysisReportResponse;
import com.example.UniBridge.analysis.dto.GpaCertificationAnalysisRequest;
import com.example.UniBridge.analysis.entity.AnalysisReport;
import com.example.UniBridge.analysis.repository.AnalysisReportRepository;
import com.example.UniBridge.certification.entity.UserCertification;
import com.example.UniBridge.certification.repository.UserCertificationRepository;
import com.example.UniBridge.company.Company;
import com.example.UniBridge.company.CompanyRepository;
import com.example.UniBridge.specification.entity.Specification;
import com.example.UniBridge.specification.repository.SpecificationRepository;
import jakarta.persistence.EntityNotFoundException;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class AnalysisService {

    private static final Long CURRENT_USER_ID = 1L;

    private final AnalysisReportRepository analysisReportRepository;
    private final SpecificationRepository specificationRepository;
    private final CompanyRepository companyRepository;
    private final UserCertificationRepository userCertificationRepository;
    private final LocalAiAnalysisService localAiAnalysisService;

    @Transactional
    public AnalysisReportResponse analyzeGpaAndCertification(GpaCertificationAnalysisRequest request) {
        if (request.getCompanyId() == null) {
            throw new IllegalArgumentException("기업 ID를 입력해 주세요.");
        }

        Specification specification = specificationRepository.findByUserId(CURRENT_USER_ID).orElse(new Specification(CURRENT_USER_ID));
        Company company = companyRepository.findById(request.getCompanyId())
                .orElseThrow(() -> new IllegalArgumentException("존재하지 않는 기업입니다."));
        List<UserCertification> userCertifications = userCertificationRepository.findByUserId(CURRENT_USER_ID);

        int gpaScore = calculateGpaScore(specification);
        int certificationScore = calculateCertificationScore(userCertifications);
        int totalScore = calculateTotalScore(gpaScore, certificationScore);
        int targetAverageScore = company.getAverageScore();
        int gapScore = targetAverageScore - totalScore;
        AiAnalysisResult aiResult = analyzeWithLocalAi(gpaScore, certificationScore, totalScore,
                targetAverageScore, gapScore, specification, company, userCertifications);
        aiResult = validateAiResult(aiResult, gpaScore, certificationScore, totalScore, targetAverageScore, gapScore);
        int aiAdjustmentScore = clamp(aiResult.getAdjustmentScore() == null ? 0 : aiResult.getAdjustmentScore(),
                -10, 10);
        int aiAdjustedScore = clamp(totalScore + aiAdjustmentScore, 0, 100);
        int finalGapScore = targetAverageScore - aiAdjustedScore;
        String aiSummary = aiResult.getSummary();
        String aiRecommendation = aiResult.getRecommendation();
        String summary = aiSummary + " " + aiRecommendation;

        AnalysisReport report = AnalysisReport.builder()
                .userId(CURRENT_USER_ID)
                .companyId(company.getId())
                .companyName(company.getName())
                .targetAverageScore(targetAverageScore)
                .gpaScore(gpaScore)
                .certificationScore(certificationScore)
                .totalScore(totalScore)
                .aiAdjustmentScore(aiAdjustmentScore)
                .aiAdjustedScore(aiAdjustedScore)
                .aiAnalysisSource(aiResult.getAnalysisSource())
                .gapScore(finalGapScore)
                .aiSummary(aiSummary)
                .aiRecommendation(aiRecommendation)
                .summary(summary)
                .build();

        return AnalysisReportResponse.from(analysisReportRepository.save(report));
    }

    @Transactional(readOnly = true)
    public List<AnalysisReportResponse> getMyAnalysisReports() {
        return analysisReportRepository.findByUserIdOrderByCreatedAtDesc(CURRENT_USER_ID).stream()
                .map(AnalysisReportResponse::from)
                .toList();
    }

    @Transactional(readOnly = true)
    public AnalysisReportResponse getAnalysisReport(Long analysisId) {
        return AnalysisReportResponse.from(analysisReportRepository.findById(analysisId)
                .orElseThrow(() -> new EntityNotFoundException("존재하지 않는 분석 결과입니다.")));
    }

    public int calculateGpaScore(Specification specification) {
        BigDecimal gpa = specification.getGpa();
        BigDecimal maxGpa = specification.getMaxGpa();
        
        if (gpa == null || maxGpa == null || maxGpa.signum() <= 0) {
            return 0; // Return 0 if GPA is missing, instead of throwing an error
        }

        int score = gpa.divide(maxGpa, 4, RoundingMode.HALF_UP)
                .multiply(BigDecimal.valueOf(100))
                .setScale(0, RoundingMode.HALF_UP)
                .intValue();
        return Math.min(score, 100);
    }

    public int calculateCertificationScore(List<UserCertification> userCertifications) {
        int score = userCertifications.stream()
                .map(UserCertification::getCertification)
                .mapToInt(certification -> certification.getScore() == null ? 0 : certification.getScore())
                .sum();
        return Math.min(score, 100);
    }

    public int calculateTotalScore(Integer gpaScore, Integer certificationScore) {
        return (int) Math.round(gpaScore * 0.6 + certificationScore * 0.4);
    }

    public String createSummary(Integer gpaScore, Integer certificationScore, Integer totalScore,
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

    private AiAnalysisResult validateAiResult(AiAnalysisResult result, int gpaScore, int certificationScore,
                                              int totalScore, int targetAverageScore, int gapScore) {
        if (result == null) {
            return fallbackAiResult(gpaScore, certificationScore, totalScore, targetAverageScore, gapScore);
        }

        return AiAnalysisResult.builder()
                .adjustmentScore(clamp(result.getAdjustmentScore() == null ? 0 : result.getAdjustmentScore(),
                        -10, 10))
                .summary(hasText(result.getSummary())
                        ? result.getSummary().trim()
                        : createSummary(gpaScore, certificationScore, totalScore, targetAverageScore, gapScore))
                .recommendation(hasText(result.getRecommendation())
                        ? result.getRecommendation().trim()
                        : "기본 점수 기준으로 부족한 항목을 보완해 주세요.")
                .analysisSource(hasText(result.getAnalysisSource())
                        ? result.getAnalysisSource().trim()
                        : LocalAiAnalysisService.SOURCE_FALLBACK)
                .build();
    }

    private AiAnalysisResult analyzeWithLocalAi(int gpaScore, int certificationScore, int totalScore,
                                                int targetAverageScore, int gapScore,
                                                Specification specification, Company company,
                                                List<UserCertification> userCertifications) {
        try {
            return localAiAnalysisService.analyzeSpec(gpaScore, certificationScore, totalScore, targetAverageScore,
                    gapScore, specification, company, userCertifications);
        } catch (Exception e) {
            return fallbackAiResult(gpaScore, certificationScore, totalScore, targetAverageScore, gapScore);
        }
    }

    private AiAnalysisResult fallbackAiResult(int gpaScore, int certificationScore, int totalScore,
                                              int targetAverageScore, int gapScore) {
        return AiAnalysisResult.builder()
                .adjustmentScore(0)
                .summary(createSummary(gpaScore, certificationScore, totalScore, targetAverageScore, gapScore))
                .recommendation("기본 점수 기준으로 부족한 항목을 보완해 주세요.")
                .analysisSource(LocalAiAnalysisService.SOURCE_FALLBACK)
                .build();
    }

    private int clamp(int value, int min, int max) {
        return Math.max(min, Math.min(max, value));
    }

    private boolean hasText(String value) {
        return value != null && !value.isBlank();
    }
}
