package com.example.UniBridge.analysis.dto;

import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class GapItemResponse {

    private String field;
    private String label;
    private String currentValue;
    private String alumnusValue;
    private String displayText;
    private Integer score;
    private String status;
    private String message;
    private String comment;
}
