package com.example.UniBridge.analysis;

import com.example.UniBridge.company.Company;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
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

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "company_id")
    private Company company;

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

    @Column(length = 2000)
    private String strengths;

    @Column(length = 2000)
    private String weaknesses;

    @Column(length = 2000)
    private String recommendations;

    @Builder
    public AnalysisReport(Long userId, Company company, String companyName, String mainJobRole, Integer userScore,
                          Integer targetAverageScore, Integer gapScore, Integer gpaScore, Integer languageScore,
                          Integer certificationScore, Integer projectScore, Integer activityScore,
                          String strengths, String weaknesses, String recommendations) {
        this.userId = userId;
        this.company = company;
        this.companyName = companyName;
        this.mainJobRole = mainJobRole;
        this.userScore = userScore;
        this.targetAverageScore = targetAverageScore;
        this.gapScore = gapScore;
        this.gpaScore = gpaScore;
        this.languageScore = languageScore;
        this.certificationScore = certificationScore;
        this.projectScore = projectScore;
        this.activityScore = activityScore;
        this.strengths = strengths;
        this.weaknesses = weaknesses;
        this.recommendations = recommendations;
    }
}
