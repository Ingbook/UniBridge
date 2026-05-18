package com.example.UniBridge.analysis;

import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class AnalysisReportDto {

    private Long id;
    private Long userId;
    private Long companyId;
    private String companyName;
    private String mainJobRole;
    private Integer userScore;
    private Integer targetAverageScore;
    private Integer gapScore;
    private Integer gpaScore;
    private Integer languageScore;
    private Integer certificationScore;
    private Integer projectScore;
    private Integer activityScore;
    private String strengths;
    private String weaknesses;
    private String recommendations;

    public static AnalysisReportDto from(AnalysisReport report) {
        return AnalysisReportDto.builder()
                .id(report.getId())
                .userId(report.getUserId())
                .companyId(report.getCompany().getId())
                .companyName(report.getCompanyName())
                .mainJobRole(report.getMainJobRole())
                .userScore(report.getUserScore())
                .targetAverageScore(report.getTargetAverageScore())
                .gapScore(report.getGapScore())
                .gpaScore(report.getGpaScore())
                .languageScore(report.getLanguageScore())
                .certificationScore(report.getCertificationScore())
                .projectScore(report.getProjectScore())
                .activityScore(report.getActivityScore())
                .strengths(report.getStrengths())
                .weaknesses(report.getWeaknesses())
                .recommendations(report.getRecommendations())
                .build();
    }
}
