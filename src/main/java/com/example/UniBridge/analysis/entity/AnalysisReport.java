package com.example.UniBridge.analysis.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import java.time.LocalDateTime;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "analysis_reports")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class AnalysisReport {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private Long userId;
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

    @Column(length = 2000)
    private String aiSummary;

    @Column(length = 2000)
    private String aiRecommendation;

    @Column(length = 2000)
    private String summary;

    private LocalDateTime createdAt;

    @Builder
    public AnalysisReport(Long userId, Long companyId, String companyName, Integer targetAverageScore,
                          Integer gpaScore, Integer certificationScore, Integer totalScore,
                          Integer aiAdjustmentScore, Integer aiAdjustedScore, String aiAnalysisSource,
                          Integer gapScore, String aiSummary, String aiRecommendation, String summary) {
        this.userId = userId;
        this.companyId = companyId;
        this.companyName = companyName;
        this.targetAverageScore = targetAverageScore;
        this.gpaScore = gpaScore;
        this.certificationScore = certificationScore;
        this.totalScore = totalScore;
        this.aiAdjustmentScore = aiAdjustmentScore;
        this.aiAdjustedScore = aiAdjustedScore;
        this.aiAnalysisSource = aiAnalysisSource;
        this.gapScore = gapScore;
        this.aiSummary = aiSummary;
        this.aiRecommendation = aiRecommendation;
        this.summary = summary;
    }

    @PrePersist
    public void prePersist() {
        this.createdAt = LocalDateTime.now();
    }
}
