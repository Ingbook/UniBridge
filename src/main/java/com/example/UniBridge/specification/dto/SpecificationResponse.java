package com.example.UniBridge.specification.dto;

import com.example.UniBridge.specification.entity.Specification;
import java.math.BigDecimal;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class SpecificationResponse {

    private Long specificationId;
    private Long userId;
    private BigDecimal gpa;
    private BigDecimal maxGpa;

    public static SpecificationResponse from(Specification specification) {
        return SpecificationResponse.builder()
                .specificationId(specification.getId())
                .userId(specification.getUserId())
                .gpa(specification.getGpa())
                .maxGpa(specification.getMaxGpa())
                .build();
    }
}
