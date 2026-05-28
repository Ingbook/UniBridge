package com.example.UniBridge.specification.dto;

import java.math.BigDecimal;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
public class SpecificationRequest {

    private BigDecimal gpa;
    private BigDecimal maxGpa;
}
