package com.example.UniBridge.analysis.dto;

import java.util.List;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class GapAnalysisResponse {

    // From your branch - simple and clean
    private AnalysisProfileResponse currentUser;
    private AnalysisProfileResponse selectedAlumnus;
    private List<GapItemResponse> gapItems;
    private FieldCommentsResponse fieldComments;
    private OverallCommentResponse overallComment;

    // From main branch - let's keep the useful parts
    private Long companyId;
    private String companyName;
    private String companyLogoUrl; // ADDED FOR THE LOGO
    private Long selectedAlumnusId;
    private String targetJobRole;
    private Integer overallScore;
    private String scoreDescription;
    private String summary;
}
