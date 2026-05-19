package com.example.UniBridge.specification;

import java.math.BigDecimal;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
public class SpecificationRequest {

    private BigDecimal gpa;
    private BigDecimal maxGpa;
    private String languageType;
    private Integer languageScore;
    private String certifications;
    private String awards;
    private String projects;
    private String internships;
    private String portfolioUrl;
}
