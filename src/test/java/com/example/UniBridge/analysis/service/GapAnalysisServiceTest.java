package com.example.UniBridge.analysis.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;

import com.example.UniBridge.alumnus.Alumnus;
import com.example.UniBridge.alumnus.AlumnusRepository;
import com.example.UniBridge.analysis.dto.ComparisonStatus;
import com.example.UniBridge.analysis.dto.GapAnalysisRequest;
import com.example.UniBridge.analysis.dto.GapAnalysisResponse;
import com.example.UniBridge.analysis.dto.OllamaGapAnalysisResult;
import com.example.UniBridge.analysis.ollama.OllamaGapAnalysisClient;
import com.example.UniBridge.certification.entity.Certification;
import com.example.UniBridge.certification.entity.UserCertification;
import com.example.UniBridge.certification.repository.UserCertificationRepository;
import com.example.UniBridge.company.Company;
import com.example.UniBridge.company.CompanyRepository;
import com.example.UniBridge.specification.entity.Specification;
import com.example.UniBridge.specification.service.SpecificationService;
import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

@ExtendWith(MockitoExtension.class)
class GapAnalysisServiceTest {

    @Mock
    private SpecificationService specificationService;

    @Mock
    private CompanyRepository companyRepository;

    @Mock
    private AlumnusRepository alumnusRepository;

    @Mock
    private UserCertificationRepository userCertificationRepository;

    @Mock
    private OllamaGapAnalysisClient ollamaGapAnalysisClient;

    private GapAnalysisService gapAnalysisService;

    @BeforeEach
    void setUp() {
        gapAnalysisService = new GapAnalysisService(
                specificationService,
                companyRepository,
                alumnusRepository,
                userCertificationRepository,
                ollamaGapAnalysisClient
        );
    }

    @Test
    void analyzeGap_throwsException_whenCompanyIdIsNull() {
        assertThatThrownBy(() -> gapAnalysisService.analyzeGap(request(null, 1L, "Backend Developer")))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("기업 ID를 입력해 주세요.");
    }

    @Test
    void analyzeGap_throwsException_whenAlumnusIdIsNull() {
        assertThatThrownBy(() -> gapAnalysisService.analyzeGap(request(1L, null, "Backend Developer")))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("동문 ID를 입력해 주세요.");
    }

    @Test
    void analyzeGap_throwsException_whenCompanyDoesNotExist() {
        when(specificationService.getMySpecificationEntityForAnalysis()).thenReturn(specification());
        when(companyRepository.findById(999L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> gapAnalysisService.analyzeGap(request(999L, 1L, "Backend Developer")))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("존재하지 않는 기업입니다.");
    }

    @Test
    void analyzeGap_throwsException_whenAlumnusDoesNotExist() {
        Company company = company();
        when(specificationService.getMySpecificationEntityForAnalysis()).thenReturn(specification());
        when(companyRepository.findById(1L)).thenReturn(Optional.of(company));
        when(alumnusRepository.findByCompanyIdAndId(1L, 999L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> gapAnalysisService.analyzeGap(request(1L, 999L, "Backend Developer")))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("존재하지 않는 동문입니다.");
    }

    @Test
    void analyzeGap_throwsException_whenUserSpecificationDoesNotExist() {
        when(specificationService.getMySpecificationEntityForAnalysis())
                .thenThrow(new IllegalArgumentException("학점 정보를 먼저 등록해 주세요."));

        assertThatThrownBy(() -> gapAnalysisService.analyzeGap(request(1L, 1L, "Backend Developer")))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("학점 정보를 먼저 등록해 주세요.");
    }

    @Test
    void analyzeGap_mapsOllamaJsonResultToResponse() {
        Company company = company();
        Alumnus alumnus = alumnus(company);
        when(specificationService.getMySpecificationEntityForAnalysis()).thenReturn(specification());
        when(companyRepository.findById(1L)).thenReturn(Optional.of(company));
        when(alumnusRepository.findByCompanyIdAndId(1L, 1L)).thenReturn(Optional.of(alumnus));
        when(userCertificationRepository.findByUserId(1L)).thenReturn(List.of(
                userCertification("정보처리기사"),
                userCertification("SQLD")
        ));
        when(ollamaGapAnalysisClient.analyze(anyString())).thenReturn(ollamaResult());

        GapAnalysisResponse response = gapAnalysisService.analyzeGap(request(1L, 1L, "Backend Developer"));

        assertThat(response.getCompanyId()).isEqualTo(1L);
        assertThat(response.getCompanyName()).isEqualTo("TechCorp");
        assertThat(response.getTargetJobRole()).isEqualTo("Backend Developer");
        assertThat(response.getTotalScore()).isEqualTo(82);
        assertThat(response.getSummary()).isEqualTo("직무 경험은 좋고 프로젝트 설명 보강이 필요합니다.");
        assertThat(response.getUserProfile().getGpa()).isEqualByComparingTo("3.8");
        assertThat(response.getUserProfile().getCertificationCount()).isEqualTo(2);
        assertThat(response.getAlumnusProfile().getLanguageScore()).isEqualTo(960);
        assertThat(response.getComparisonItems()).hasSize(5);
        assertThat(response.getDetailAnalysis().getStrengths()).contains("지원 직무와 연결되는 경험이 명확합니다.");
        assertThat(response.getDetailAnalysis().getWeaknesses()).contains("프로젝트 성과와 문제 해결 과정 설명이 부족합니다.");
        assertThat(response.getDetailAnalysis().getComments()).hasSize(3);
    }

    @Test
    void analyzeGap_returnsFallbackResponse_whenOllamaFails() {
        Company company = company();
        when(specificationService.getMySpecificationEntityForAnalysis()).thenReturn(specification());
        when(companyRepository.findById(1L)).thenReturn(Optional.of(company));
        when(alumnusRepository.findByCompanyIdAndId(1L, 1L)).thenReturn(Optional.of(alumnus(company)));
        when(userCertificationRepository.findByUserId(1L)).thenReturn(List.of());
        when(ollamaGapAnalysisClient.analyze(anyString())).thenThrow(new RuntimeException("connection failed"));

        GapAnalysisResponse response = gapAnalysisService.analyzeGap(request(1L, 1L, "Backend Developer"));

        assertThat(response.getTotalScore()).isEqualTo(60);
        assertThat(response.getComparisonItems()).hasSize(5);
        assertThat(response.getDetailAnalysis().getWeaknesses()).contains("AI 상세 판단을 완료하지 못했습니다.");
        assertThat(response.getSummary()).contains("Ollama 연결 또는 응답 해석에 실패했습니다.");
    }

    private GapAnalysisRequest request(Long companyId, Long alumnusId, String targetJobRole) {
        GapAnalysisRequest request = new GapAnalysisRequest();
        ReflectionTestUtils.setField(request, "companyId", companyId);
        ReflectionTestUtils.setField(request, "alumnusId", alumnusId);
        ReflectionTestUtils.setField(request, "targetJobRole", targetJobRole);
        return request;
    }

    private Specification specification() {
        return Specification.builder()
                .userId(1L)
                .gpa(new BigDecimal("3.8"))
                .maxGpa(new BigDecimal("4.5"))
                .build();
    }

    private Company company() {
        Company company = Company.builder()
                .name("TechCorp")
                .industry("IT")
                .mainJobRole("Backend Developer")
                .averageScore(85)
                .alumnusCount(3)
                .location("Seoul")
                .build();
        ReflectionTestUtils.setField(company, "id", 1L);
        return company;
    }

    private Alumnus alumnus(Company company) {
        Alumnus alumnus = Alumnus.builder()
                .company(company)
                .name("James Kim")
                .jobRole("Backend Developer")
                .gpa(new BigDecimal("4.2"))
                .maxGpa(new BigDecimal("4.5"))
                .languageType("TOEIC")
                .languageScore(960)
                .certificationCount(5)
                .awardCount(3)
                .projectSummary("실서비스 배포 경험과 데이터 기반 추천 프로젝트 보유")
                .portfolioLevel("상")
                .profileImageUrl("")
                .representativeScore(82)
                .build();
        ReflectionTestUtils.setField(alumnus, "id", 1L);
        return alumnus;
    }

    private UserCertification userCertification(String name) {
        Certification certification = Certification.builder()
                .name(name)
                .category("BACKEND")
                .score(30)
                .description("설명")
                .build();
        return UserCertification.builder()
                .userId(1L)
                .certification(certification)
                .build();
    }

    private OllamaGapAnalysisResult ollamaResult() {
        OllamaGapAnalysisResult result = new OllamaGapAnalysisResult();
        result.setTotalScore(82);
        result.setScoreDescription("상위 동문 평균과 가까운 준비도입니다.");
        result.setSummary("직무 경험은 좋고 프로젝트 설명 보강이 필요합니다.");
        result.setStrengths(List.of("지원 직무와 연결되는 경험이 명확합니다."));
        result.setWeaknesses(List.of("프로젝트 성과와 문제 해결 과정 설명이 부족합니다."));
        result.setComments(List.of(
                "현재 스펙은 기본 경쟁력을 갖추고 있습니다.",
                "다만 프로젝트 설명을 수치와 결과 중심으로 보완해야 합니다.",
                "동문 대비 어학성적과 포트폴리오 완성도를 우선 개선하는 것이 좋습니다."
        ));
        result.setItems(List.of(
                item("GPA", "3.8/4.5", "4.2/4.5", "동문보다 0.4 낮습니다.", 78),
                item("Language", "TOEIC 850", "TOEIC 960", "동문보다 110점 낮습니다.", 76),
                item("Certifications", "2개", "5개", "동문보다 3개 적습니다.", 70),
                item("Awards", "1개", "3개", "동문보다 2개 적습니다.", 72),
                item("ProjectPortfolio", "AI 기반 취업 분석 서비스 개발", "실서비스 배포 경험과 데이터 기반 추천 프로젝트 보유",
                        "설명 구체성이 부족합니다.", 80)
        ));
        return result;
    }

    private OllamaGapAnalysisResult.Item item(String category, String userValue, String alumnusValue,
                                              String gapDescription, Integer aiScore) {
        OllamaGapAnalysisResult.Item item = new OllamaGapAnalysisResult.Item();
        item.setCategory(category);
        item.setUserValue(userValue);
        item.setAlumnusValue(alumnusValue);
        item.setGapDescription(gapDescription);
        item.setAiScore(aiScore);
        item.setStatus(ComparisonStatus.NEEDS_IMPROVEMENT);
        return item;
    }
}
