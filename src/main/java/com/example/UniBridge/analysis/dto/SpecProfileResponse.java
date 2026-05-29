package com.example.UniBridge.analysis.dto;

import java.math.BigDecimal;
import java.util.List;
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
    private List<String> certificationNames;
    private Integer awardCount;
    private String projectSummary;
    private String portfolioDescription;
    private String portfolioLevel;
}
