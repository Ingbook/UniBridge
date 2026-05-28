package com.example.UniBridge.certification.dto;

import com.example.UniBridge.certification.entity.Certification;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class CertificationResponse {

    private Long certificationId;
    private String name;
    private String category;
    private Integer score;
    private String description;

    public static CertificationResponse from(Certification certification) {
        return CertificationResponse.builder()
                .certificationId(certification.getId())
                .name(certification.getName())
                .category(certification.getCategory())
                .score(certification.getScore())
                .description(certification.getDescription())
                .build();
    }
}
