package com.example.UniBridge.analysis.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.example.UniBridge.analysis.dto.AnalysisReportResponse;
import com.example.UniBridge.analysis.dto.GpaCertificationAnalysisRequest;
import com.example.UniBridge.analysis.entity.AnalysisReport;
import com.example.UniBridge.analysis.repository.AnalysisReportRepository;
import com.example.UniBridge.certification.entity.Certification;
import com.example.UniBridge.certification.entity.UserCertification;
import com.example.UniBridge.certification.repository.UserCertificationRepository;
import com.example.UniBridge.company.Company;
import com.example.UniBridge.company.CompanyRepository;
import com.example.UniBridge.specification.entity.Specification;
import com.example.UniBridge.specification.service.SpecificationService;
import jakarta.persistence.EntityNotFoundException;
import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

@ExtendWith(MockitoExtension.class)
class AnalysisServiceTest {

    @Mock
    private AnalysisReportRepository analysisReportRepository;

    @Mock
    private SpecificationService specificationService;

    @Mock
    private CompanyRepository companyRepository;

    @Mock
    private UserCertificationRepository userCertificationRepository;

    @InjectMocks
    private AnalysisService analysisService;

    @Test
    void calculateGpaScore_roundsAndLimitsToHundred() {
        Specification specification = specification("3.8", "4.5");

        int gpaScore = analysisService.calculateGpaScore(specification);

        assertThat(gpaScore).isEqualTo(84);
    }

    @Test
    void calculateGpaScore_throwsException_whenGpaIsInvalid() {
        assertThatThrownBy(() -> analysisService.calculateGpaScore(specification(null, "4.5")))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("학점을 입력해 주세요.");
        assertThatThrownBy(() -> analysisService.calculateGpaScore(specification("3.0", "0")))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("최대 학점은 0보다 커야 합니다.");
        assertThatThrownBy(() -> analysisService.calculateGpaScore(specification("-1", "4.5")))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("학점은 0 이상이어야 합니다.");
        assertThatThrownBy(() -> analysisService.calculateGpaScore(specification("4.6", "4.5")))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("학점은 최대 학점보다 클 수 없습니다.");
    }

    @Test
    void calculateCertificationScore_sumsCertificationScoresAndLimitsToHundred() {
        List<UserCertification> userCertifications = List.of(
                userCertification(certification("A", 60)),
                userCertification(certification("B", 50))
        );

        int certificationScore = analysisService.calculateCertificationScore(userCertifications);

        assertThat(certificationScore).isEqualTo(100);
    }

    @Test
    void calculateTotalScore_appliesGpaAndCertificationWeights() {
        int totalScore = analysisService.calculateTotalScore(84, 50);

        assertThat(totalScore).isEqualTo(70);
    }

    @Test
    void createSummary_addsBaseAndWeaknessMessages() {
        String summary = analysisService.createSummary(60, 30, 48, 85, 37);

        assertThat(summary)
                .contains("목표 기업 평균 점수보다 낮습니다. 학점 또는 자격증 보완이 필요합니다.")
                .contains("학점 점수 보완이 필요합니다.")
                .contains("직무 관련 자격증을 추가하면 좋습니다.");
    }

    @Test
    void analyzeGpaAndCertification_calculatesAndSavesAnalysisReport() {
        Specification specification = specification("3.8", "4.5");
        Company company = company("TechCorp", 85);
        List<UserCertification> userCertifications = List.of(
                userCertification(certification("정보처리기사", 30)),
                userCertification(certification("SQLD", 20))
        );
        GpaCertificationAnalysisRequest request = analysisRequest(1L);

        when(specificationService.getMySpecificationEntityForAnalysis()).thenReturn(specification);
        when(companyRepository.findById(1L)).thenReturn(Optional.of(company));
        when(userCertificationRepository.findByUserId(1L)).thenReturn(userCertifications);
        when(analysisReportRepository.save(any(AnalysisReport.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        AnalysisReportResponse response = analysisService.analyzeGpaAndCertification(request);

        assertThat(response.getCompanyId()).isEqualTo(1L);
        assertThat(response.getCompanyName()).isEqualTo("TechCorp");
        assertThat(response.getTargetAverageScore()).isEqualTo(85);
        assertThat(response.getGpaScore()).isEqualTo(84);
        assertThat(response.getCertificationScore()).isEqualTo(50);
        assertThat(response.getTotalScore()).isEqualTo(70);
        assertThat(response.getGapScore()).isEqualTo(15);
        verify(analysisReportRepository).save(any(AnalysisReport.class));
    }

    @Test
    void analyzeGpaAndCertification_throwsException_whenCompanyDoesNotExist() {
        GpaCertificationAnalysisRequest request = analysisRequest(999L);
        when(specificationService.getMySpecificationEntityForAnalysis()).thenReturn(specification("3.8", "4.5"));
        when(companyRepository.findById(999L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> analysisService.analyzeGpaAndCertification(request))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("존재하지 않는 기업입니다.");
    }

    @Test
    void getAnalysisReport_throwsException_whenReportDoesNotExist() {
        when(analysisReportRepository.findById(999L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> analysisService.getAnalysisReport(999L))
                .isInstanceOf(EntityNotFoundException.class)
                .hasMessage("존재하지 않는 분석 결과입니다.");
    }

    private Specification specification(String gpa, String maxGpa) {
        return Specification.builder()
                .userId(1L)
                .gpa(gpa == null ? null : new BigDecimal(gpa))
                .maxGpa(maxGpa == null ? null : new BigDecimal(maxGpa))
                .build();
    }

    private Certification certification(String name, Integer score) {
        return Certification.builder()
                .name(name)
                .category("BACKEND")
                .score(score)
                .description("설명")
                .build();
    }

    private UserCertification userCertification(Certification certification) {
        return UserCertification.builder()
                .userId(1L)
                .certification(certification)
                .build();
    }

    private Company company(String name, Integer averageScore) {
        Company company = Company.builder()
                .name(name)
                .industry("IT")
                .mainJobRole("Backend Developer")
                .averageScore(averageScore)
                .build();
        ReflectionTestUtils.setField(company, "id", 1L);
        return company;
    }

    private GpaCertificationAnalysisRequest analysisRequest(Long companyId) {
        GpaCertificationAnalysisRequest request = new GpaCertificationAnalysisRequest();
        ReflectionTestUtils.setField(request, "companyId", companyId);
        return request;
    }
}
