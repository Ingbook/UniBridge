package com.example.UniBridge.specification.dto;

import com.example.UniBridge.specification.entity.Specification;
import java.math.BigDecimal;
import java.util.List;
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
    private List<String> certificationNames;
    private Integer certificationCount;

    public static SpecificationResponse from(Specification specification, List<String> certificationNames) {
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
                .certificationNames(certificationNames)
                .certificationCount(certificationNames.size())
                .build();
    }
}
