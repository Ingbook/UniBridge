package com.example.UniBridge.analysis.service;

import static org.assertj.core.api.Assertions.assertThat;

import com.example.UniBridge.analysis.dto.AiProfileAnalysisResponse;
import java.math.BigDecimal;
import java.util.List;
import org.junit.jupiter.api.Test;

class LocalAiAnalysisServiceTest {

    private final LocalAiAnalysisService service = new LocalAiAnalysisService(null);

    @Test
    void validateProfileResult_removesCertificationsThatUserDoesNotOwn() {
        AiProfileAnalysisResponse.UserProfile userProfile = userProfile(List.of("정보처리기사"));
        AiProfileAnalysisResponse.AiAnalysis aiResult = AiProfileAnalysisResponse.AiAnalysis.builder()
                .strengths(List.of(
                        "정보처리기사 자격증 보유로 개발 기본 역량을 보여줄 수 있습니다.",
                        "SQLD 자격증 보유로 데이터베이스 활용 능력이 좋습니다.",
                        "ADsP 자격증 보유로 데이터 분석 역량이 있습니다.",
                        "AWS Cloud Practitioner 자격증 보유로 클라우드 역량이 있습니다.",
                        "컴퓨터활용능력 1급 자격증 보유로 컴퓨터 활용 능력이 좋습니다."
                ))
                .weaknesses(List.of("프로젝트 설명 보완 필요: 역할과 성과를 더 구체적으로 작성해 주세요."))
                .comment("현재 정보처리기사, SQLD, ADsP, AWS Cloud Practitioner, 컴퓨터활용능력 1급 자격증을 보유하고 있습니다.")
                .build();

        AiProfileAnalysisResponse.AiAnalysis result =
                service.validateProfileResult(aiResult, userProfile, LocalAiAnalysisService.SOURCE_OLLAMA);

        assertThat(result.getStrengths())
                .containsExactly("정보처리기사 자격증 보유로 개발 기본 역량을 보여줄 수 있습니다.");
        assertThat(result.getComment())
                .contains("현재 보유 자격증은 정보처리기사 총 1개입니다.")
                .doesNotContain("SQLD", "ADsP", "AWS Cloud Practitioner", "컴퓨터활용능력 1급");
    }

    @Test
    void validateProfileResult_doesNotMentionSpecificCertification_whenUserHasNone() {
        AiProfileAnalysisResponse.UserProfile userProfile = userProfile(List.of());
        AiProfileAnalysisResponse.AiAnalysis aiResult = AiProfileAnalysisResponse.AiAnalysis.builder()
                .strengths(List.of("SQLD 자격증 보유로 데이터베이스 활용 능력이 좋습니다."))
                .weaknesses(List.of("프로젝트 설명 보완 필요"))
                .comment("SQLD 자격증을 보유하고 있어 강점입니다.")
                .build();

        AiProfileAnalysisResponse.AiAnalysis result =
                service.validateProfileResult(aiResult, userProfile, LocalAiAnalysisService.SOURCE_OLLAMA);

        assertThat(result.getStrengths()).noneMatch(value -> value.contains("SQLD"));
        assertThat(result.getComment())
                .contains("현재 입력된 보유 자격증은 없습니다.")
                .doesNotContain("SQLD");
    }

    private AiProfileAnalysisResponse.UserProfile userProfile(List<String> certifications) {
        return AiProfileAnalysisResponse.UserProfile.builder()
                .name("현재 사용자")
                .gpa(new BigDecimal("3.0"))
                .language(AiProfileAnalysisResponse.Language.builder()
                        .type("TOEIC")
                        .score(700)
                        .displayText("TOEIC 700")
                        .build())
                .certifications(AiProfileAnalysisResponse.Certifications.builder()
                        .items(certifications)
                        .count(certifications.size())
                        .build())
                .awardCount(1)
                .project("게임프로젝트")
                .build();
    }
}
