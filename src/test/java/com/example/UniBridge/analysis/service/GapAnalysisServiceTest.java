package com.example.UniBridge.analysis.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.example.UniBridge.alumnus.Alumnus;
import com.example.UniBridge.alumnus.AlumnusRepository;
import com.example.UniBridge.analysis.dto.FieldComment;
import com.example.UniBridge.analysis.dto.GapItemResponse;
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
import org.mockito.ArgumentCaptor;
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
                ollamaGapAnalysisClient,
                new AnalysisScoreCalculator()
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

        assertThat(response.getCurrentUser().getName()).isEqualTo("현재 사용자");
        assertThat(response.getCurrentUser().getGpa()).isEqualByComparingTo("3.8");
        assertThat(response.getCurrentUser().getCertifications().getItems()).containsExactly("정보처리기사", "SQLD");
        assertThat(response.getCurrentUser().getCertifications().getCount()).isEqualTo(2);
        assertThat(response.getSelectedAlumnus().getName()).isEqualTo("James Kim");
        assertThat(response.getSelectedAlumnus().getLanguage().getScore()).isEqualTo(960);
        assertThat(response.getSelectedAlumnus().getCertifications().getItems())
                .containsExactly("정보처리기사", "SQLD", "ADsP", "AWS Cloud Practitioner");
        assertThat(response.getScoreDescription()).isEqualTo("상위 동문 평균과 가까운 준비도입니다.");
        assertThat(response.getSummarized()).contains("AI 기반 취업 분석 서비스 개발");
        assertThat(response.getSummary()).isEqualTo("직무 관련 자격증과 프로젝트 경험은 있으나, 동문 대비 수상경력과 어학성적 보완이 필요합니다.");
        assertThat(response.getGapItems()).hasSize(6);
        assertThat(response.getGapItems())
                .extracting(GapItemResponse::getField)
                .containsExactly("gpa", "language", "certifications", "awardCount", "project", "portfolio");
        assertThat(response.getFieldComments().getName().getMessage()).isNotBlank();
        assertFixedFieldComments(response);
        assertThat(findItem(response, "project").getDisplayText())
                .isEqualTo("AI 기반 취업 분석 서비스 개발 → 실서비스 배포 경험과 데이터 기반 추천 프로젝트 보유");
        assertThat(findItem(response, "project").getComment()).isNotBlank();
        assertThat(response.getOverallComment().getStrengths()).contains("직무와 연결되는 프로젝트 경험이 있습니다.");
        assertThat(response.getOverallComment().getWeaknesses())
                .contains("프로젝트 성과와 배포 경험 설명을 더 구체화할 필요가 있습니다.");
        assertThat(response.getOverallComment().getAiComment()).contains("현재 스펙은 기본 경쟁력을 갖추고 있습니다.");
    }

    @Test
    void analyzeGap_includesCertificationNamesCountAndAwardCountInPrompt() {
        Company company = company();
        when(specificationService.getMySpecificationEntityForAnalysis()).thenReturn(specification());
        when(companyRepository.findById(1L)).thenReturn(Optional.of(company));
        when(alumnusRepository.findByCompanyIdAndId(1L, 1L)).thenReturn(Optional.of(alumnus(company)));
        when(userCertificationRepository.findByUserId(1L)).thenReturn(List.of(
                userCertification("정보처리기사"),
                userCertification("SQLD")
        ));
        when(ollamaGapAnalysisClient.analyze(anyString())).thenReturn(ollamaResult());
        ArgumentCaptor<String> promptCaptor = ArgumentCaptor.forClass(String.class);

        gapAnalysisService.analyzeGap(request(1L, 1L, "Backend Developer"));

        verify(ollamaGapAnalysisClient).analyze(promptCaptor.capture());
        assertThat(promptCaptor.getValue())
                .contains("\"certificationCount\":2")
                .contains("\"certificationNames\":[\"정보처리기사\",\"SQLD\"]")
                .contains("\"awardCount\":1")
                .contains("\"certificationCount\":4")
                .contains("\"certificationNames\":[\"정보처리기사\",\"SQLD\",\"ADsP\",\"AWS Cloud Practitioner\"]")
                .contains("\"awardCount\":3")
                .contains("CERTIFICATION은 자격증 이름과 목표 직무 관련성을 반드시 함께 평가한다")
                .contains("AWARD는 수상 개수를 기준으로 비교한다");
    }

    @Test
    void analyzeGap_throwsException_whenUserCertificationIsNotAllowed() {
        Company company = company();
        when(specificationService.getMySpecificationEntityForAnalysis()).thenReturn(specification());
        when(companyRepository.findById(1L)).thenReturn(Optional.of(company));
        when(alumnusRepository.findByCompanyIdAndId(1L, 1L)).thenReturn(Optional.of(alumnus(company)));
        when(userCertificationRepository.findByUserId(1L)).thenReturn(List.of(
                userCertification("네트워크관리사")
        ));

        assertThatThrownBy(() -> gapAnalysisService.analyzeGap(request(1L, 1L, "Backend Developer")))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("입력 가능한 자격증은 정보처리기사, SQLD, ADsP, AWS Cloud Practitioner, 리눅스마스터 2급, 컴퓨터활용능력 1급입니다.");
    }

    @Test
    void analyzeGap_mapsAwardCountToComparisonItemValue() {
        GapAnalysisResponse response = analyzeSuccessfully(specification());

        GapItemResponse awardItem = findItem(response, "awardCount");

        assertThat(awardItem.getLabel()).isEqualTo("수상경력");
        assertThat(awardItem.getCurrentValue()).isEqualTo("1개");
        assertThat(awardItem.getAlumnusValue()).isEqualTo("3개");
        assertThat(awardItem.getDisplayText()).isEqualTo("1개 → 3개");
        assertThat(awardItem.getScore()).isEqualTo(65);
        assertThat(awardItem.getMessage()).isNotBlank();
        assertThat(awardItem.getComment()).isNotBlank();
    }

    @Test
    void analyzeGap_usesActualProjectSummary_whenProjectSummaryExists() {
        GapAnalysisResponse response = analyzeSuccessfully(specification("AI 기반 취업 분석 서비스 개발", "실제 포트폴리오 설명"));

        assertThat(response.getCurrentUser().getProject()).isEqualTo("AI 기반 취업 분석 서비스 개발");
        assertThat(response.getCurrentUser().getPortfolio()).isEqualTo("실제 포트폴리오 설명");
        assertThat(findItem(response, "project").getCurrentValue())
                .isEqualTo("AI 기반 취업 분석 서비스 개발");
        assertThat(findItem(response, "portfolio").getCurrentValue())
                .isEqualTo("실제 포트폴리오 설명");
    }

    @Test
    void analyzeGap_usesProjectFallbackOnlyWhenProjectSummaryIsBlank() {
        GapAnalysisResponse response = analyzeSuccessfully(specification(" ", null));

        assertThat(response.getCurrentUser().getProject()).isEqualTo("설명 보완 필요");
        assertThat(response.getCurrentUser().getPortfolio()).isEqualTo("설명 보완 필요");
        assertThat(findItem(response, "project").getCurrentValue()).isEqualTo("설명 보완 필요");
        assertThat(findItem(response, "portfolio").getCurrentValue()).isEqualTo("설명 보완 필요");
    }

    @Test
    void analyzeGap_mapsUppercaseCategoriesWithoutMissingScores() {
        GapAnalysisResponse response = analyzeSuccessfully(specification());

        assertThat(findItem(response, "certifications").getScore()).isBetween(0, 100);
        assertThat(findItem(response, "awardCount").getScore()).isBetween(0, 100);
        assertThat(findItem(response, "project").getScore()).isBetween(0, 100);
        assertThat(findItem(response, "portfolio").getScore()).isBetween(0, 100);
        assertFixedFieldComments(response);
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

        assertThat(response.getGapItems()).hasSize(6);
        assertThat(response.getOverallComment().getWeaknesses()).contains("AI 상세 판단을 완료하지 못했습니다.");
        assertThat(response.getSummary()).isNotBlank();
        assertFixedFieldComments(response);
    }

    private GapAnalysisResponse analyzeSuccessfully(Specification specification) {
        Company company = company();
        when(specificationService.getMySpecificationEntityForAnalysis()).thenReturn(specification);
        when(companyRepository.findById(1L)).thenReturn(Optional.of(company));
        when(alumnusRepository.findByCompanyIdAndId(1L, 1L)).thenReturn(Optional.of(alumnus(company)));
        when(userCertificationRepository.findByUserId(1L)).thenReturn(List.of(
                userCertification("정보처리기사"),
                userCertification("SQLD")
        ));
        when(ollamaGapAnalysisClient.analyze(anyString())).thenReturn(ollamaResult());

        return gapAnalysisService.analyzeGap(request(1L, 1L, "Backend Developer"));
    }

    private GapItemResponse findItem(GapAnalysisResponse response, String field) {
        return response.getGapItems().stream()
                .filter(item -> field.equals(item.getField()))
                .findFirst()
                .orElseThrow();
    }

    private void assertFixedFieldComments(GapAnalysisResponse response) {
        assertComment(response.getFieldComments().getName());
        assertComment(response.getFieldComments().getGpa());
        assertComment(response.getFieldComments().getLanguage());
        assertComment(response.getFieldComments().getCertifications());
        assertComment(response.getFieldComments().getAwardCount());
        assertComment(response.getFieldComments().getProject());
        assertComment(response.getFieldComments().getPortfolio());
    }

    private void assertComment(FieldComment comment) {
        assertThat(comment.getLabel()).isNotBlank();
        assertThat(comment.getMessage()).isNotBlank();
        assertThat(comment.getComment()).isNotBlank();
    }

    private GapAnalysisRequest request(Long companyId, Long alumnusId, String targetJobRole) {
        GapAnalysisRequest request = new GapAnalysisRequest();
        ReflectionTestUtils.setField(request, "companyId", companyId);
        ReflectionTestUtils.setField(request, "alumnusId", alumnusId);
        ReflectionTestUtils.setField(request, "targetJobRole", targetJobRole);
        return request;
    }

    private Specification specification() {
        return specification("AI 기반 취업 분석 서비스 개발", "실제 포트폴리오 설명");
    }

    private Specification specification(String projectSummary, String portfolioDescription) {
        return Specification.builder()
                .userId(1L)
                .gpa(new BigDecimal("3.8"))
                .maxGpa(new BigDecimal("4.5"))
                .awardCount(1)
                .projectSummary(projectSummary)
                .portfolioDescription(portfolioDescription)
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
                .certificationSummary("정보처리기사, SQLD, ADsP, AWS Cloud Practitioner, 컴퓨터활용능력")
                .awardCount(3)
                .projectSummary("실서비스 배포 경험과 데이터 기반 추천 프로젝트 보유")
                .portfolioDescription("상")
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
        result.setSummary("직무 관련 자격증과 프로젝트 경험은 있으나, 동문 대비 수상경력과 어학성적 보완이 필요합니다.");
        result.setStrengths(List.of(
                "직무와 연결되는 프로젝트 경험이 있습니다.",
                "기본적인 직무 관련 자격증을 보유하고 있습니다."
        ));
        result.setWeaknesses(List.of(
                "동문 대비 자격증 범위와 수상경력 개수가 부족합니다.",
                "프로젝트 성과와 배포 경험 설명을 더 구체화할 필요가 있습니다."
        ));
        result.setComments(List.of(
                "현재 스펙은 기본 경쟁력을 갖추고 있습니다.",
                "자격증은 단순 개수보다 목표 직무와 연결되는 종류를 추가하는 것이 좋습니다.",
                "프로젝트 설명에는 사용 기술, 본인 역할, 문제 해결 과정, 결과 수치를 포함해 보완하세요."
        ));
        result.setItems(List.of(
                item("GPA", "3.8/4.5", "4.2/4.5", "동문보다 학점이 0.4 낮습니다.", 78, "NEEDS_IMPROVEMENT"),
                item("LANGUAGE", "TOEIC 850", "TOEIC 960", "어학성적은 동문 대비 보완이 필요합니다.", 75,
                        "NEEDS_IMPROVEMENT"),
                item("CERTIFICATION", "정보처리기사, SQLD / 총 2개", "정보처리기사, SQLD, ADsP 외 2개 / 총 5개",
                        "직무 관련 자격증은 보유하고 있으나 개수와 범위는 동문보다 부족합니다.", 70,
                        "NEEDS_IMPROVEMENT"),
                item("AWARD", "1개", "3개", "수상경력 개수는 동문보다 적습니다.", 65,
                        "NEEDS_IMPROVEMENT"),
                item("PROJECT_PORTFOLIO", "AI 기반 취업 분석 서비스 개발", "실서비스 배포 경험과 데이터 기반 추천 프로젝트 보유",
                        "프로젝트 주제는 직무와 연결되지만 성과와 배포 경험 설명을 더 구체화해야 합니다.", 80,
                        "GOOD")
        ));
        return result;
    }

    private OllamaGapAnalysisResult.Item item(String category, String userValue, String alumnusValue,
                                              String gapDescription, Integer aiScore, String status) {
        OllamaGapAnalysisResult.Item item = new OllamaGapAnalysisResult.Item();
        item.setCategory(category);
        item.setUserValue(userValue);
        item.setAlumnusValue(alumnusValue);
        item.setGapDescription(gapDescription);
        item.setAiScore(aiScore);
        item.setStatus(status);
        return item;
    }
}
