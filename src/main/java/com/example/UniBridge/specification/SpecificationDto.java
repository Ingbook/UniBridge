package com.example.UniBridge.specification;

import java.math.BigDecimal;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class SpecificationDto {

    private Long id;
    private Long userId;
    private BigDecimal gpa;
    private BigDecimal maxGpa;
    private String languageType;
    private Integer languageScore;
    private String certifications;
    private String awards;
    private String projects;
    private String internships;
    private String portfolioUrl;

    public static SpecificationDto from(Specification specification) {
        return SpecificationDto.builder()
                .id(specification.getId())
                .userId(specification.getUserId())
                .gpa(specification.getGpa())
                .maxGpa(specification.getMaxGpa())
                .languageType(specification.getLanguageType())
                .languageScore(specification.getLanguageScore())
                .certifications(specification.getCertifications())
                .awards(specification.getAwards())
                .projects(specification.getProjects())
                .internships(specification.getInternships())
                .portfolioUrl(specification.getPortfolioUrl())
                .build();
    }
}
