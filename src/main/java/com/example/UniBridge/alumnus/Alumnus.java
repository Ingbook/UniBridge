package com.example.UniBridge.alumnus;

import com.example.UniBridge.company.Company;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import java.math.BigDecimal;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "alumnus")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Alumnus {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "company_id")
    private Company company;

    private String name;
    private String jobRole;
    private BigDecimal gpa;
    private BigDecimal maxGpa;
    private String languageType;
    private Integer languageScore;
    private Integer certificationCount;
    private String certificationSummary;
    private Integer awardCount;
    private String projectSummary;
    private String portfolioDescription;
    private String portfolioLevel;
    private String profileImageUrl;
    private Integer representativeScore;

    @Builder
    public Alumnus(Company company, String name, String jobRole, BigDecimal gpa, BigDecimal maxGpa,
                   String languageType, Integer languageScore, Integer certificationCount, Integer awardCount,
                   String certificationSummary, String projectSummary, String portfolioDescription,
                   String portfolioLevel, String profileImageUrl, Integer representativeScore) {
        this.company = company;
        this.name = name;
        this.jobRole = jobRole;
        this.gpa = gpa;
        this.maxGpa = maxGpa;
        this.languageType = languageType;
        this.languageScore = languageScore;
        this.certificationCount = certificationCount;
        this.certificationSummary = certificationSummary;
        this.awardCount = awardCount;
        this.projectSummary = projectSummary;
        this.portfolioDescription = portfolioDescription;
        this.portfolioLevel = portfolioLevel;
        this.profileImageUrl = profileImageUrl;
        this.representativeScore = representativeScore;
    }
}
