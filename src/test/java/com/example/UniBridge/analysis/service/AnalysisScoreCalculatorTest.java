package com.example.UniBridge.analysis.service;

import static org.assertj.core.api.Assertions.assertThat;

import com.example.UniBridge.analysis.dto.ComparisonItemResponse;
import com.example.UniBridge.analysis.dto.SpecProfileResponse;
import java.math.BigDecimal;
import java.util.List;
import org.junit.jupiter.api.Test;

class AnalysisScoreCalculatorTest {

    private final AnalysisScoreCalculator calculator = new AnalysisScoreCalculator();

    @Test
    void calculate_returnsDifferentScoresForDifferentAlumniProfiles() {
        SpecProfileResponse user = user("AI 기반 취업 분석 서비스에서 Spring API와 React 화면을 개발하고 배포했습니다.",
                "GitHub README와 배포 링크를 포함한 포트폴리오를 정리했습니다.");

        int strongAlumnusScore = calculator.calculate(user, alumnus("Strong", "4.3", 980, 4, 3,
                "컴퓨터 비전 모델 학습과 운영 자동화 프로젝트를 수행했습니다.",
                "모델 성능과 배포 과정을 정리한 포트폴리오")).overallScore();
        int similarAlumnusScore = calculator.calculate(user, alumnus("Similar", "3.8", 850, 2, 1,
                "AI 기반 취업 분석 서비스에서 Spring API를 개발했습니다.",
                "GitHub와 배포 링크를 정리한 포트폴리오")).overallScore();

        assertThat(strongAlumnusScore).isNotEqualTo(similarAlumnusScore);
    }

    @Test
    void calculate_changesOverallScoreAndGapItemsWhenSelectedAlumnusChanges() {
        SpecProfileResponse user = user("AI 기반 취업 분석 서비스에서 추천 API를 개발했습니다.",
                "프로젝트 구조와 실행 방법을 정리한 포트폴리오");

        AnalysisScoreCalculator.AnalysisScoreResult first = calculator.calculate(user,
                alumnus("James Kim", "4.2", 960, 4, 3,
                        "실서비스 배포 경험과 데이터 기반 추천 프로젝트 보유",
                        "운영 지표를 포함한 포트폴리오"));
        AnalysisScoreCalculator.AnalysisScoreResult second = calculator.calculate(user,
                alumnus("Sarah Lee", "3.7", 850, 2, 1,
                        "AI 기반 취업 분석 서비스에서 추천 API를 개발했습니다.",
                        "프로젝트 구조를 정리한 포트폴리오"));

        assertThat(first.overallScore()).isNotEqualTo(second.overallScore());
        assertThat(scoreOf(first, "GPA")).isNotEqualTo(scoreOf(second, "GPA"));
    }

    @Test
    void calculate_usesDescriptionFallbackOnlyWhenProjectStringIsBlankOrTooShort() {
        SpecProfileResponse blankProjectUser = user("짧음", "포트폴리오 설명도 충분히 작성했습니다.");
        SpecProfileResponse describedProjectUser = user("Spring API와 React 화면을 개발하고 AWS에 배포했습니다.",
                "포트폴리오 설명도 충분히 작성했습니다.");
        SpecProfileResponse alumnus = alumnus("James Kim", "4.0", 900, 3, 2,
                "Spring API와 React 화면을 개발하고 AWS에 배포했습니다.",
                "포트폴리오 설명도 충분히 작성했습니다.");

        assertThat(itemOf(calculator.calculate(blankProjectUser, alumnus), "PROJECT").getUserValue())
                .isEqualTo("설명 보완 필요");
        assertThat(calculator.calculate(blankProjectUser, alumnus).summarized())
                .contains("프로젝트 설명 보완 필요");
        assertThat(itemOf(calculator.calculate(describedProjectUser, alumnus), "PROJECT").getUserValue())
                .contains("Spring API와 React 화면");
        assertThat(calculator.calculate(describedProjectUser, alumnus).summarized())
                .contains("Spring API와 React 화면");
    }

    @Test
    void calculate_ignoresCertificationNamesOutsideAllowedList() {
        SpecProfileResponse user = profile("현재 사용자", "3.8", 850,
                List.of("정보처리기사", "허용되지않은자격증", "SQLD"), 1,
                "Spring API 개발 프로젝트를 수행했습니다.", "포트폴리오를 정리했습니다.");
        SpecProfileResponse alumnus = alumnus("James Kim", "4.0", 900, 3, 1,
                "Spring API 개발 프로젝트를 수행했습니다.", "포트폴리오를 정리했습니다.");

        ComparisonItemResponse item = itemOf(calculator.calculate(user, alumnus), "CERTIFICATION");

        assertThat(item.getUserValue()).contains("정보처리기사", "SQLD", "인정 자격증 2개");
        assertThat(item.getUserValue()).doesNotContain("허용되지않은자격증");
    }

    @Test
    void calculate_certificationIsExcellent_whenUserHasRecognizedCertificationsAndAlumnusHasNone() {
        SpecProfileResponse user = profile("현재 사용자", "3.8", 850,
                List.of("정보처리기사", "SQLD"), 1,
                "Spring API 개발 프로젝트를 수행했습니다.", "포트폴리오를 정리했습니다.");
        SpecProfileResponse alumnus = profile("James Kim", "3.8", 850, List.of(), 1,
                "Spring API 개발 프로젝트를 수행했습니다.", "포트폴리오를 정리했습니다.");

        ComparisonItemResponse item = itemOf(calculator.calculate(user, alumnus), "CERTIFICATION");

        assertThat(item.getScore()).isGreaterThanOrEqualTo(90);
        assertThat(item.getStatus()).isEqualTo("EXCELLENT");
        assertThat(item.getComment()).isEqualTo("사용자가 선택 동문보다 인정 자격증을 더 많이 보유하고 있습니다.");
    }

    @Test
    void calculate_certificationNeedsImprovement_whenUserHasNoneAndAlumnusHasRecognizedCertifications() {
        SpecProfileResponse user = profile("현재 사용자", "3.8", 850, List.of(), 1,
                "Spring API 개발 프로젝트를 수행했습니다.", "포트폴리오를 정리했습니다.");
        SpecProfileResponse alumnus = profile("James Kim", "3.8", 850,
                List.of("정보처리기사", "SQLD"), 1,
                "Spring API 개발 프로젝트를 수행했습니다.", "포트폴리오를 정리했습니다.");

        ComparisonItemResponse item = itemOf(calculator.calculate(user, alumnus), "CERTIFICATION");

        assertThat(item.getStatus()).isEqualTo("NEEDS_IMPROVEMENT");
        assertThat(item.getComment()).isEqualTo("선택 동문보다 인정 자격증 수가 부족해 보완이 필요합니다.");
    }

    @Test
    void calculate_certificationIsStrong_whenUserHasMoreRecognizedCertificationsThanAlumnus() {
        SpecProfileResponse user = profile("현재 사용자", "3.8", 850,
                List.of("정보처리기사", "SQLD"), 1,
                "Spring API 개발 프로젝트를 수행했습니다.", "포트폴리오를 정리했습니다.");
        SpecProfileResponse alumnus = profile("James Kim", "3.8", 850,
                List.of("정보처리기사"), 1,
                "Spring API 개발 프로젝트를 수행했습니다.", "포트폴리오를 정리했습니다.");

        ComparisonItemResponse item = itemOf(calculator.calculate(user, alumnus), "CERTIFICATION");

        assertThat(item.getScore()).isGreaterThanOrEqualTo(85);
        assertThat(item.getStatus()).isIn("EXCELLENT", "GOOD");
        assertThat(item.getComment()).isEqualTo("사용자가 선택 동문보다 인정 자격증을 더 많이 보유하고 있습니다.");
    }

    @Test
    void calculate_certificationUsesNeutralScore_whenBothHaveNoRecognizedCertifications() {
        SpecProfileResponse user = profile("현재 사용자", "3.8", 850, List.of("없는자격증"), 1,
                "Spring API 개발 프로젝트를 수행했습니다.", "포트폴리오를 정리했습니다.");
        SpecProfileResponse alumnus = profile("James Kim", "3.8", 850, List.of(), 1,
                "Spring API 개발 프로젝트를 수행했습니다.", "포트폴리오를 정리했습니다.");

        ComparisonItemResponse item = itemOf(calculator.calculate(user, alumnus), "CERTIFICATION");

        assertThat(item.getScore()).isEqualTo(100);
        assertThat(item.getStatus()).isEqualTo("EXCELLENT");
        assertThat(item.getUserValue()).isEqualTo("인정 자격증 0개");
    }

    @Test
    void calculate_marksGpaAndLanguageAsNeedsImprovementWhenUserValueIsLowerThanAlumnus() {
        SpecProfileResponse user = profile("현재 사용자", "3.8", 850,
                List.of("정보처리기사", "SQLD"), 1,
                "Spring API 개발 프로젝트를 수행했습니다.", "포트폴리오를 정리했습니다.");
        SpecProfileResponse alumnus = profile("James Kim", "4.3", 970,
                List.of("정보처리기사", "SQLD"), 1,
                "Spring API 개발 프로젝트를 수행했습니다.", "포트폴리오를 정리했습니다.");

        ComparisonItemResponse gpa = itemOf(calculator.calculate(user, alumnus), "GPA");
        ComparisonItemResponse language = itemOf(calculator.calculate(user, alumnus), "LANGUAGE");

        assertThat(gpa.getScore()).isEqualTo(88);
        assertThat(gpa.getStatus()).isEqualTo("NEEDS_IMPROVEMENT");
        assertThat(gpa.getComment()).isEqualTo("선택 동문보다 0.5점 낮습니다.");
        assertThat(language.getScore()).isEqualTo(88);
        assertThat(language.getStatus()).isEqualTo("NEEDS_IMPROVEMENT");
        assertThat(language.getComment()).isEqualTo("선택 동문보다 120점 낮습니다.");
    }

    @Test
    void calculate_marksGpaAndLanguageAsExcellentOnlyWhenUserValueMeetsOrExceedsAlumnus() {
        SpecProfileResponse user = profile("현재 사용자", "4.3", 970,
                List.of("정보처리기사", "SQLD"), 1,
                "Spring API 개발 프로젝트를 수행했습니다.", "포트폴리오를 정리했습니다.");
        SpecProfileResponse alumnus = profile("James Kim", "3.8", 850,
                List.of("정보처리기사", "SQLD"), 1,
                "Spring API 개발 프로젝트를 수행했습니다.", "포트폴리오를 정리했습니다.");

        ComparisonItemResponse gpa = itemOf(calculator.calculate(user, alumnus), "GPA");
        ComparisonItemResponse language = itemOf(calculator.calculate(user, alumnus), "LANGUAGE");

        assertThat(gpa.getScore()).isEqualTo(100);
        assertThat(gpa.getStatus()).isEqualTo("EXCELLENT");
        assertThat(gpa.getComment()).isEqualTo("선택 동문 기준과 비교해 충분히 경쟁력 있는 항목입니다.");
        assertThat(language.getScore()).isEqualTo(100);
        assertThat(language.getStatus()).isEqualTo("EXCELLENT");
        assertThat(language.getComment()).isEqualTo("선택 동문 기준과 비교해 충분히 경쟁력 있는 항목입니다.");
    }

    @Test
    void validCertificationNames_parsesCommaSlashWhitespaceAndCase() {
        assertThat(calculator.validCertificationNamesFromText("정보처리기사, SQLD"))
                .containsExactly("정보처리기사", "SQLD");
        assertThat(calculator.validCertificationNamesFromText("정보처리기사 / SQLD"))
                .containsExactly("정보처리기사", "SQLD");
        assertThat(calculator.validCertificationNamesFromText("정보처리기사,SQLD"))
                .containsExactly("정보처리기사", "SQLD");
        assertThat(calculator.validCertificationNamesFromText(" aws cloud practitioner / adsp "))
                .containsExactly("AWS Cloud Practitioner", "ADsP");
    }

    @Test
    void calculate_certificationTreatsNullAlumnusCertificationFieldAsZeroWithoutNpe() {
        SpecProfileResponse user = profile("현재 사용자", "3.8", 850, List.of("정보처리기사"), 1,
                "Spring API 개발 프로젝트를 수행했습니다.", "포트폴리오를 정리했습니다.");
        SpecProfileResponse alumnus = SpecProfileResponse.builder()
                .name("James Kim")
                .profileImageUrl("")
                .gpa(new BigDecimal("3.8"))
                .maxGpa(new BigDecimal("4.5"))
                .languageType("TOEIC")
                .languageScore(850)
                .certificationCount(null)
                .certificationNames(null)
                .awardCount(1)
                .projectSummary("Spring API 개발 프로젝트를 수행했습니다.")
                .portfolioDescription("포트폴리오를 정리했습니다.")
                .portfolioLevel("기본")
                .build();

        ComparisonItemResponse item = itemOf(calculator.calculate(user, alumnus), "CERTIFICATION");

        assertThat(item.getAlumnusValue()).isEqualTo("인정 자격증 0개");
        assertThat(item.getScore()).isGreaterThanOrEqualTo(90);
        assertThat(item.getStatus()).isEqualTo("EXCELLENT");
    }

    @Test
    void calculate_clampsScoresBetweenZeroAndOneHundred() {
        SpecProfileResponse user = profile("현재 사용자", "4.5", 1500,
                List.of("정보처리기사", "SQLD", "ADsP", "AWS Cloud Practitioner", "리눅스마스터 2급", "컴퓨터활용능력 1급"),
                20,
                "AI 데이터 분석 Spring React Python API 배포 운영 클라우드 프로젝트를 상세히 작성했습니다.",
                "AI 데이터 분석 Spring React Python API 배포 운영 클라우드 포트폴리오를 상세히 작성했습니다.");
        SpecProfileResponse alumnus = alumnus("James Kim", "2.0", 500, 1, 1, "짧은 프로젝트", "짧은 포트폴리오");

        AnalysisScoreCalculator.AnalysisScoreResult result = calculator.calculate(user, alumnus);

        assertThat(result.overallScore()).isBetween(0, 100);
        assertThat(result.gapItems())
                .extracting(ComparisonItemResponse::getScore)
                .allSatisfy(score -> assertThat(score).isBetween(0, 100));
    }

    private int scoreOf(AnalysisScoreCalculator.AnalysisScoreResult result, String category) {
        return itemOf(result, category).getScore();
    }

    private ComparisonItemResponse itemOf(AnalysisScoreCalculator.AnalysisScoreResult result, String category) {
        return result.gapItems().stream()
                .filter(item -> category.equals(item.getCategory()))
                .findFirst()
                .orElseThrow();
    }

    private SpecProfileResponse user(String projectSummary, String portfolioDescription) {
        return profile("현재 사용자", "3.8", 850, List.of("정보처리기사", "SQLD"), 1,
                projectSummary, portfolioDescription);
    }

    private SpecProfileResponse alumnus(String name, String gpa, Integer languageScore, Integer certificationCount,
                                        Integer awardCount, String projectSummary, String portfolioDescription) {
        List<String> certifications = List.of(
                "정보처리기사", "SQLD", "ADsP", "AWS Cloud Practitioner", "리눅스마스터 2급", "컴퓨터활용능력 1급"
        ).stream().limit(certificationCount).toList();
        return profile(name, gpa, languageScore, certifications, awardCount, projectSummary, portfolioDescription);
    }

    private SpecProfileResponse profile(String name, String gpa, Integer languageScore, List<String> certifications,
                                        Integer awardCount, String projectSummary, String portfolioDescription) {
        List<String> safeCertifications = certifications == null ? List.of() : certifications;
        return SpecProfileResponse.builder()
                .name(name)
                .profileImageUrl("")
                .gpa(new BigDecimal(gpa))
                .maxGpa(new BigDecimal("4.5"))
                .languageType("TOEIC")
                .languageScore(languageScore)
                .certificationCount(safeCertifications.size())
                .certificationNames(safeCertifications)
                .awardCount(awardCount)
                .projectSummary(calculator.normalizeDescription(projectSummary))
                .portfolioDescription(calculator.normalizeDescription(portfolioDescription))
                .portfolioLevel("기본")
                .build();
    }
}
