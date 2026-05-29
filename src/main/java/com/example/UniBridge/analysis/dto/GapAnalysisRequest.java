package com.example.UniBridge.analysis.dto;

import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
public class GapAnalysisRequest {

    private Long companyId;
    private Long alumnusId;
    private String targetJobRole;
}
