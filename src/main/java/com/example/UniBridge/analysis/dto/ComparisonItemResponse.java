package com.example.UniBridge.analysis.dto;

import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class ComparisonItemResponse {

    private String category;
    private String displayName;
    private String userValue;
    private String alumnusValue;
    private String gapDescription;
    private Integer aiScore;
    private String status;
}
