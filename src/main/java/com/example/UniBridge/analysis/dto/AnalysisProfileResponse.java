package com.example.UniBridge.analysis.dto;

import java.math.BigDecimal;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class AnalysisProfileResponse {

    private String name;
    private BigDecimal gpa;
    private LanguageValue language;
    private CertificationValue certifications;
    private Integer awardCount;
    private String project;
    private String portfolio;
}
