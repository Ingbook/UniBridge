package com.example.UniBridge.specification;

import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Column;
import jakarta.persistence.Table;
import java.math.BigDecimal;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "specifications")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Specification {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private Long userId;
    private BigDecimal gpa;
    private BigDecimal maxGpa;
    private String languageType;
    private Integer languageScore;

    @Column(length = 2000)
    private String certifications;

    @Column(length = 2000)
    private String awards;

    @Column(length = 4000)
    private String projects;

    @Column(length = 2000)
    private String internships;

    @Column(length = 1000)
    private String portfolioUrl;

    @Builder
    public Specification(Long userId, BigDecimal gpa, BigDecimal maxGpa, String languageType, Integer languageScore,
                         String certifications, String awards, String projects, String internships, String portfolioUrl) {
        this.userId = userId;
        update(gpa, maxGpa, languageType, languageScore, certifications, awards, projects, internships, portfolioUrl);
    }

    public void update(BigDecimal gpa, BigDecimal maxGpa, String languageType, Integer languageScore,
                       String certifications, String awards, String projects, String internships, String portfolioUrl) {
        this.gpa = gpa;
        this.maxGpa = maxGpa;
        this.languageType = languageType;
        this.languageScore = languageScore;
        this.certifications = certifications;
        this.awards = awards;
        this.projects = projects;
        this.internships = internships;
        this.portfolioUrl = portfolioUrl;
    }
}
