package com.example.UniBridge.analysis.dto;

import java.math.BigDecimal;
import java.util.List;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
public class AiProfileAnalysisRequest {

    private String name;
    private BigDecimal gpa;
    private String languageType;
    private Integer languageScore;
    private List<String> certifications;
    private Integer awardCount;
    private String project;
}
