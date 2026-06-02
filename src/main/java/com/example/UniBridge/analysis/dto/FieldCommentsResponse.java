package com.example.UniBridge.analysis.dto;

import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class FieldCommentsResponse {

    private FieldComment name;
    private FieldComment gpa;
    private FieldComment language;
    private FieldComment certifications;
    private FieldComment awardCount;
    private FieldComment project;
    private FieldComment portfolio;
}
