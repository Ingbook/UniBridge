package com.example.UniBridge.analysis.dto;

import java.math.BigDecimal;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class SpecProfileResponse {

    private String name;
    private String profileImageUrl;
    private BigDecimal gpa;
    private BigDecimal maxGpa;
    private String languageType;
    private Integer languageScore;
    private Integer certificationCount;
    private Integer awardCount;
    private String projectSummary;
    private String portfolioLevel;
}
