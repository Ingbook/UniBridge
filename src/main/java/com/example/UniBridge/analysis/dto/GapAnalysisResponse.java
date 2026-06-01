package com.example.UniBridge.analysis.dto;

import java.util.List;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class GapAnalysisResponse {

    private Long companyId;
    private String companyName;
    private Long selectedAlumnusId;
    private String selectedAlumnusName;
    private String targetJobRole;
    private Integer overallScore;
    private Integer totalScore;
    private String scoreDescription;
    private String summarized;
    private String summary;
    private SpecProfileResponse userProfile;
    private SpecProfileResponse selectedAlumnusProfile;
    private SpecProfileResponse alumnusProfile;
    private List<ComparisonItemResponse> gapItems;
    private List<ComparisonItemResponse> comparisonItems;
    private AiDetailAnalysisResponse detailAnalysis;
}
