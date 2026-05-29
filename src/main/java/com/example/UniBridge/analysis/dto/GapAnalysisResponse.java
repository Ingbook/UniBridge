package com.example.UniBridge.analysis.dto;

import java.util.List;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class GapAnalysisResponse {

    private Long companyId;
    private String companyName;
    private String targetJobRole;
    private Integer totalScore;
    private String scoreDescription;
    private String summary;
    private SpecProfileResponse userProfile;
    private SpecProfileResponse alumnusProfile;
    private List<ComparisonItemResponse> comparisonItems;
    private AiDetailAnalysisResponse detailAnalysis;
}
