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
        return CompanyDto.builder()
                .id(company.getId())
                .name(company.getName())
                .industry(company.getIndustry())
                .mainJobRole(company.getMainJobRole())
                .averageScore(company.getAverageScore())
                .alumnusCount(company.getAlumnusCount() == null ? 0 : company.getAlumnusCount())
                .location(company.getLocation() == null ? "Seoul" : company.getLocation())
                .build();
    }
}
