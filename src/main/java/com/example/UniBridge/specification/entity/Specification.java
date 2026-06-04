package com.example.UniBridge.specification.entity;

import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import java.math.BigDecimal;
import java.time.LocalDateTime;
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
    private Integer awardCount;
    private String projectSummary;
    private String portfolioDescription;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    @Builder
    public Specification(Long userId) {
        this.userId = userId;
    }

    public void update(BigDecimal gpa, BigDecimal maxGpa, String languageType, Integer languageScore,
                       Integer awardCount, String projectSummary, String portfolioDescription) {
        this.gpa = gpa;
        this.maxGpa = maxGpa;
        this.languageType = languageType;
        this.languageScore = languageScore;
        this.awardCount = awardCount;
        this.projectSummary = projectSummary;
        this.portfolioDescription = portfolioDescription;
    }

    @PrePersist
    public void prePersist() {
        LocalDateTime now = LocalDateTime.now();
        this.createdAt = now;
        this.updatedAt = now;
    }

    @PreUpdate
    public void preUpdate() {
        this.updatedAt = LocalDateTime.now();
    }
}
