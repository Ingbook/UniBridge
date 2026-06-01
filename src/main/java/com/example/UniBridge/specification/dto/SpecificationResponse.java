package com.example.UniBridge.specification.dto;

import com.example.UniBridge.specification.entity.Specification;
import java.math.BigDecimal;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class SpecificationResponse {

    private Long specificationId;
    private Long userId;
    private BigDecimal gpa;
    private BigDecimal maxGpa;
    private String languageType;
    private Integer languageScore;
    private Integer awardCount;
    private String projectSummary;
    private String portfolioDescription;

    public static SpecificationResponse from(Specification specification) {
        return SpecificationResponse.builder()
                .specificationId(specification.getId())
                .userId(specification.getUserId())
                .gpa(specification.getGpa())
                .maxGpa(specification.getMaxGpa())
                .languageType(specification.getLanguageType())
                .languageScore(specification.getLanguageScore())
                .awardCount(specification.getAwardCount())
                .projectSummary(specification.getProjectSummary())
                .portfolioDescription(specification.getPortfolioDescription())
                .build();
    }
}
