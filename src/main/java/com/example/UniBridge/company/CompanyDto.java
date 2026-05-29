package com.example.UniBridge.company;

import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class CompanyDto {

    private Long id;
    private String name;
    private String industry;
    private String mainJobRole;
    private Integer averageScore;
    private Integer alumnusCount;
    private String location;

    public static CompanyDto from(Company company) {
        return from(company, company.getAlumnusCount());
    }

    public static CompanyDto from(Company company, Long alumnusCount) {
        return from(company, alumnusCount == null ? null : alumnusCount.intValue());
    }

    public static CompanyDto from(Company company, Integer alumnusCount) {
        return CompanyDto.builder()
                .id(company.getId())
                .name(company.getName())
                .industry(company.getIndustry())
                .mainJobRole(company.getMainJobRole())
                .averageScore(company.getAverageScore())
                .alumnusCount(alumnusCount == null ? 0 : alumnusCount)
                .location(company.getLocation() == null ? "Seoul" : company.getLocation())
                .build();
    }
}
