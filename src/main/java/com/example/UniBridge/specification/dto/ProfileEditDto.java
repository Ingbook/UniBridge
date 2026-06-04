package com.example.UniBridge.specification.dto;

import com.example.UniBridge.specification.entity.Specification;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;
import java.util.List;

@Getter
@Setter
public class ProfileEditDto {
    private BigDecimal gpa;
    private BigDecimal maxGpa;
    private Integer languageScore;
    private Integer awardCount;
    private String projectSummary;
    private String portfolioDescription;
    private List<Long> certificationIds;

    public static ProfileEditDto from(Specification spec, List<Long> certificationIds) {
        ProfileEditDto dto = new ProfileEditDto();
        dto.setGpa(spec.getGpa());
        dto.setMaxGpa(spec.getMaxGpa());
        dto.setLanguageScore(spec.getLanguageScore());
        dto.setAwardCount(spec.getAwardCount());
        dto.setProjectSummary(spec.getProjectSummary());
        dto.setPortfolioDescription(spec.getPortfolioDescription());
        dto.setCertificationIds(certificationIds);
        return dto;
    }
}
