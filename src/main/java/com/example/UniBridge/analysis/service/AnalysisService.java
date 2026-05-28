package com.example.UniBridge.analysis.service;

import com.example.UniBridge.analysis.dto.AnalysisReportResponse;
import com.example.UniBridge.analysis.dto.GpaCertificationAnalysisRequest;
import com.example.UniBridge.analysis.entity.AnalysisReport;
import com.example.UniBridge.analysis.repository.AnalysisReportRepository;
import com.example.UniBridge.certification.entity.UserCertification;
import com.example.UniBridge.certification.repository.UserCertificationRepository;
import com.example.UniBridge.company.Company;
import com.example.UniBridge.company.CompanyRepository;
import com.example.UniBridge.specification.entity.Specification;
import com.example.UniBridge.specification.service.SpecificationService;
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
    private final SpecificationService specificationService;
    private final CompanyRepository companyRepository;
    private final UserCertificationRepository userCertificationRepository;

    @Transactional
    public AnalysisReportResponse analyzeGpaAndCertification(GpaCertificationAnalysisRequest request) {
        if (request.getCompanyId() == null) {
            throw new IllegalArgumentException("기업 ID를 입력해 주세요.");
        }

        Specification specification = specificationService.getMySpecificationEntityForAnalysis();
        Company company = companyRepository.findById(request.getCompanyId())
                .orElseThrow(() -> new IllegalArgumentException("존재하지 않는 기업입니다."));
        List<UserCertification> userCertifications = userCertificationRepository.findByUserId(CURRENT_USER_ID);

        int gpaScore = calculateGpaScore(specification);
        int certificationScore = calculateCertificationScore(userCertifications);
        int totalScore = calculateTotalScore(gpaScore, certificationScore);
        int targetAverageScore = company.getAverageScore();
        int gapScore = targetAverageScore - totalScore;
        String summary = createSummary(gpaScore, certificationScore, totalScore, targetAverageScore, gapScore);

        AnalysisReport report = AnalysisReport.builder()
                .userId(CURRENT_USER_ID)
                .companyId(company.getId())
                .companyName(company.getName())
                .targetAverageScore(targetAverageScore)
                .gpaScore(gpaScore)
                .certificationScore(certificationScore)
                .totalScore(totalScore)
                .gapScore(gapScore)
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
        validateGpa(gpa, maxGpa);

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

    private void validateGpa(BigDecimal gpa, BigDecimal maxGpa) {
        if (gpa == null) {
            throw new IllegalArgumentException("학점을 입력해 주세요.");
        }
        if (maxGpa == null || maxGpa.signum() <= 0) {
            throw new IllegalArgumentException("최대 학점은 0보다 커야 합니다.");
        }
        if (gpa.signum() < 0) {
            throw new IllegalArgumentException("학점은 0 이상이어야 합니다.");
        }
        if (gpa.compareTo(maxGpa) > 0) {
            throw new IllegalArgumentException("학점은 최대 학점보다 클 수 없습니다.");
        }
    }
}
