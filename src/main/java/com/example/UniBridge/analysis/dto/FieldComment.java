package com.example.UniBridge.analysis.dto;

import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class FieldComment {

    private String label;
    private String message;
    private String comment;
}
