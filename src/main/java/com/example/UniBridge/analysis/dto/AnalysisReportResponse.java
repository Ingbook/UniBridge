package com.example.UniBridge.analysis.dto;

import com.example.UniBridge.analysis.entity.AnalysisReport;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class AnalysisReportResponse {

    private Long analysisId;
    private Long companyId;
    private String companyName;
    private Integer targetAverageScore;
    private Integer gpaScore;
    private Integer certificationScore;
    private Integer totalScore;
    private Integer aiAdjustmentScore;
    private Integer aiAdjustedScore;
    private String aiAnalysisSource;
    private Integer gapScore;
    private String aiSummary;
    private String aiRecommendation;
    private String summary;

    public static AnalysisReportResponse from(AnalysisReport report) {
        return AnalysisReportResponse.builder()
                .analysisId(report.getId())
                .companyId(report.getCompanyId())
                .companyName(report.getCompanyName())
                .targetAverageScore(report.getTargetAverageScore())
                .gpaScore(report.getGpaScore())
                .certificationScore(report.getCertificationScore())
                .totalScore(report.getTotalScore())
                .aiAdjustmentScore(report.getAiAdjustmentScore())
                .aiAdjustedScore(report.getAiAdjustedScore())
                .aiAnalysisSource(report.getAiAnalysisSource())
                .gapScore(report.getGapScore())
                .aiSummary(report.getAiSummary())
                .aiRecommendation(report.getAiRecommendation())
                .summary(report.getSummary())
                .build();
    }
}
