package com.example.UniBridge.analysis.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.example.UniBridge.analysis.dto.AiProfileAnalysisRequest;
import com.example.UniBridge.analysis.dto.AiProfileAnalysisResponse;
import com.example.UniBridge.certification.entity.Certification;
import com.example.UniBridge.certification.repository.CertificationRepository;
import com.example.UniBridge.certification.repository.UserCertificationRepository;
import com.example.UniBridge.specification.entity.Specification;
import com.example.UniBridge.specification.repository.SpecificationRepository;
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
class ProfileAnalysisServiceTest {

    @Mock
    private LocalAiAnalysisService localAiAnalysisService;

    @Mock
    private SpecificationRepository specificationRepository;

    @Mock
    private CertificationRepository certificationRepository;

    @Mock
    private UserCertificationRepository userCertificationRepository;

    private ProfileAnalysisService profileAnalysisService;

    @BeforeEach
    void setUp() {
        profileAnalysisService = new ProfileAnalysisService(
                localAiAnalysisService,
                specificationRepository,
                certificationRepository,
                userCertificationRepository
        );
    }

    @Test
    void analyzeProfile_returnsRequestedJsonShapeAndSavesProfile() {
        AiProfileAnalysisRequest request = request("3.8", "TOEIC", 850,
                List.of("정보처리기사", "SQLD"), 1, "AI 기반 취업 분석 서비스 개발", "GitHub와 배포 링크 포함");
        AiProfileAnalysisResponse.AiAnalysis aiResponse = analysis();
        when(specificationRepository.findByUserId(1L)).thenReturn(Optional.of(specification()));
        when(certificationRepository.findByName("정보처리기사")).thenReturn(Optional.of(certification("정보처리기사")));
        when(certificationRepository.findByName("SQLD")).thenReturn(Optional.of(certification("SQLD")));
        when(localAiAnalysisService.analyzeProfile(any())).thenReturn(aiResponse);
        ArgumentCaptor<Specification> specificationCaptor = ArgumentCaptor.forClass(Specification.class);

        AiProfileAnalysisResponse response = profileAnalysisService.analyzeProfile(request);

        assertThat(response.getUserProfile().getName()).isEqualTo("현재 사용자");
        assertThat(response.getUserProfile().getGpa()).isEqualByComparingTo("3.8");
        assertThat(response.getUserProfile().getLanguage().getType()).isEqualTo("TOEIC");
        assertThat(response.getUserProfile().getLanguage().getScore()).isEqualTo(850);
        assertThat(response.getUserProfile().getLanguage().getDisplayText()).isEqualTo("TOEIC 850");
        assertThat(response.getUserProfile().getCertifications().getItems()).containsExactly("정보처리기사", "SQLD");
        assertThat(response.getUserProfile().getCertifications().getCount()).isEqualTo(2);
        assertThat(response.getUserProfile().getAwardCount()).isEqualTo(1);
        assertThat(response.getUserProfile().getProject()).isEqualTo("AI 기반 취업 분석 서비스 개발");
        assertThat(response.getUserProfile().getPortfolio()).isEqualTo("GitHub와 배포 링크 포함");
        assertThat(response.getAiAnalysis()).isSameAs(aiResponse);

        verify(specificationRepository).save(specificationCaptor.capture());
        assertThat(specificationCaptor.getValue().getProjectSummary()).isEqualTo("AI 기반 취업 분석 서비스 개발");
        assertThat(specificationCaptor.getValue().getPortfolioDescription()).isEqualTo("GitHub와 배포 링크 포함");
        verify(userCertificationRepository).deleteByUserId(1L);
        verify(userCertificationRepository, times(2)).save(any());
    }

    @Test
    void analyzeProfile_removesDuplicateCertificationsBeforeSavingAndAnalysis() {
        AiProfileAnalysisRequest request = request("3.8", "TOEIC", 850,
                List.of("정보처리기사", "SQLD", "정보처리기사"), 1, "프로젝트 설명", "포트폴리오 설명");
        when(specificationRepository.findByUserId(1L)).thenReturn(Optional.empty());
        when(certificationRepository.findByName("정보처리기사")).thenReturn(Optional.of(certification("정보처리기사")));
        when(certificationRepository.findByName("SQLD")).thenReturn(Optional.of(certification("SQLD")));
        when(localAiAnalysisService.analyzeProfile(any())).thenReturn(analysis());
        ArgumentCaptor<AiProfileAnalysisResponse.UserProfile> captor =
                ArgumentCaptor.forClass(AiProfileAnalysisResponse.UserProfile.class);

        profileAnalysisService.analyzeProfile(request);

        verify(localAiAnalysisService).analyzeProfile(captor.capture());
        assertThat(captor.getValue().getCertifications().getItems()).containsExactly("정보처리기사", "SQLD");
        assertThat(captor.getValue().getCertifications().getCount()).isEqualTo(2);
        assertThat(captor.getValue().getPortfolio()).isEqualTo("포트폴리오 설명");
    }

    @Test
    void analyzeProfile_throwsException_whenGpaIsOutOfRange() {
        AiProfileAnalysisRequest request = request("4.6", "TOEIC", 850,
                List.of("정보처리기사"), 1, "프로젝트");

        assertThatThrownBy(() -> profileAnalysisService.analyzeProfile(request))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("학점은 0.0 이상 4.5 이하로 입력해 주세요.");
    }

    @Test
    void analyzeProfile_throwsException_whenLanguageTypeIsBlank() {
        AiProfileAnalysisRequest request = request("3.8", " ", 850,
                List.of("정보처리기사"), 1, "프로젝트");

        assertThatThrownBy(() -> profileAnalysisService.analyzeProfile(request))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("어학 시험 종류를 입력해 주세요.");
    }

    @Test
    void analyzeProfile_throwsException_whenLanguageScoreIsNotPositive() {
        AiProfileAnalysisRequest request = request("3.8", "TOEIC", 0,
                List.of("정보처리기사"), 1, "프로젝트");

        assertThatThrownBy(() -> profileAnalysisService.analyzeProfile(request))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("어학 점수는 0보다 큰 숫자로 입력해 주세요.");
    }

    @Test
    void analyzeProfile_throwsException_whenAwardCountIsNegative() {
        AiProfileAnalysisRequest request = request("3.8", "TOEIC", 850,
                List.of("정보처리기사"), -1, "프로젝트");

        assertThatThrownBy(() -> profileAnalysisService.analyzeProfile(request))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("수상경력 개수는 0 이상이어야 합니다.");
    }

    @Test
    void analyzeProfile_throwsException_whenCertificationIsNotAllowed() {
        AiProfileAnalysisRequest request = request("3.8", "TOEIC", 850,
                List.of("정보처리기사", "네트워크관리사"), 1, "프로젝트");

        assertThatThrownBy(() -> profileAnalysisService.analyzeProfile(request))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("입력 가능한 자격증은 정보처리기사, SQLD, ADsP, AWS Cloud Practitioner, 리눅스마스터 2급, 컴퓨터활용능력 1급입니다.");
    }

    @Test
    void analyzeProfile_returnsFallback_whenLocalAiFails() {
        AiProfileAnalysisRequest request = request("3.8", "TOEIC", 850,
                List.of("정보처리기사"), 1, "");
        when(specificationRepository.findByUserId(1L)).thenReturn(Optional.of(specification()));
        when(certificationRepository.findByName("정보처리기사")).thenReturn(Optional.of(certification("정보처리기사")));
        doThrow(new RuntimeException("ollama unavailable"))
                .when(localAiAnalysisService)
                .analyzeProfile(any());

        AiProfileAnalysisResponse response = profileAnalysisService.analyzeProfile(request);

        assertThat(response.getAiAnalysis().getWeaknesses()).anyMatch(value -> value.contains("프로젝트 설명 보완 필요"));
        assertThat(response.getAiAnalysis().getComment()).contains("프로젝트 설명 보완 필요");
    }

    @Test
    void analyzeProfile_returnsPortfolioFallback_whenPortfolioIsBlank() {
        AiProfileAnalysisRequest request = request("3.8", "TOEIC", 850,
                List.of("정보처리기사"), 1, "프로젝트 설명", " ");
        when(specificationRepository.findByUserId(1L)).thenReturn(Optional.of(specification()));
        when(certificationRepository.findByName("정보처리기사")).thenReturn(Optional.of(certification("정보처리기사")));
        doThrow(new RuntimeException("ollama unavailable"))
                .when(localAiAnalysisService)
                .analyzeProfile(any());

        AiProfileAnalysisResponse response = profileAnalysisService.analyzeProfile(request);

        assertThat(response.getUserProfile().getPortfolio()).isEmpty();
        assertThat(response.getAiAnalysis().getWeaknesses()).anyMatch(value -> value.contains("포트폴리오 설명 보완 필요"));
        assertThat(response.getAiAnalysis().getComment()).contains("포트폴리오 설명 보완 필요");
    }

    private AiProfileAnalysisRequest request(String gpa, String languageType, Integer languageScore,
                                             List<String> certifications, Integer awardCount, String project) {
        return request(gpa, languageType, languageScore, certifications, awardCount, project, "포트폴리오 설명");
    }

    private AiProfileAnalysisRequest request(String gpa, String languageType, Integer languageScore,
                                             List<String> certifications, Integer awardCount, String project,
                                             String portfolio) {
        AiProfileAnalysisRequest request = new AiProfileAnalysisRequest();
        ReflectionTestUtils.setField(request, "gpa", gpa == null ? null : new BigDecimal(gpa));
        ReflectionTestUtils.setField(request, "languageType", languageType);
        ReflectionTestUtils.setField(request, "languageScore", languageScore);
        ReflectionTestUtils.setField(request, "certifications", certifications);
        ReflectionTestUtils.setField(request, "awardCount", awardCount);
        ReflectionTestUtils.setField(request, "project", project);
        ReflectionTestUtils.setField(request, "portfolio", portfolio);
        return request;
    }

    private AiProfileAnalysisResponse.AiAnalysis analysis() {
        return AiProfileAnalysisResponse.AiAnalysis.builder()
                .strengths(List.of("학점이 준수합니다."))
                .weaknesses(List.of("프로젝트 설명이 더 구체적이면 좋습니다."))
                .comment("현재 프로필은 백엔드/데이터 직무 지원에 활용하기 좋은 구성을 가지고 있습니다.")
                .build();
    }

    private Specification specification() {
        return Specification.builder()
                .userId(1L)
                .gpa(new BigDecimal("3.5"))
                .maxGpa(new BigDecimal("4.5"))
                .build();
    }

    private Certification certification(String name) {
        return Certification.builder()
                .name(name)
                .category("IT")
                .score(30)
                .description("설명")
                .build();
    }
}
