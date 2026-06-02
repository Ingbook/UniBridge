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
    private String logo;

    public static CompanyHomeDto from(Company company, Integer alumnusCount) {
        return CompanyHomeDto.builder()
                .id(company.getId())
                .name(company.getName())
                .category(company.getIndustry())
                .alumnusCount(alumnusCount != null ? alumnusCount
                        : company.getAlumnusCount() == null ? 0 : company.getAlumnusCount())
                .location(company.getLocation() == null ? "Seoul" : company.getLocation())
                .detailUrl("/analysis/" + company.getId())
                .logo(getLogoPath(company.getName()))
                .build();
    }

    private static String getLogoPath(String companyName) {
        return switch (companyName) {
            case "DataMind" -> "/images/logo_datamind.png";
            case "VisionLab" -> "/images/logo_visionlab.png";
            case "DeepVision" -> "/images/logo_deepvision.png";
            case "QuantumSoft" -> "/images/logo_quantumsoft.png";
            case "NovaPlatform" -> "/images/logo_novaplatform.png";
            default -> "/images/cat-cat-cat-cat.jpeg"; // Default logo
        };
    }
}
