package com.example.UniBridge.company;

import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class CompanyHomeDto {

    private Long id;
    private String name;
    private String category;  // industry
    private Integer alumnusCount;
    private String location;
    private String detailUrl;

    public static CompanyHomeDto from(Company company, Integer alumnusCount) {
        return CompanyHomeDto.builder()
                .id(company.getId())
                .name(company.getName())
                .category(company.getIndustry())
                .alumnusCount(alumnusCount != null ? alumnusCount
                        : company.getAlumnusCount() == null ? 0 : company.getAlumnusCount())
                .location(company.getLocation() == null ? "Seoul" : company.getLocation())
                .detailUrl("/analysis/" + company.getId())
                .build();
    }
}
