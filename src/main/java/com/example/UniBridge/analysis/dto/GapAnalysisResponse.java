package com.example.UniBridge.analysis.dto;

import java.util.List;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class GapAnalysisResponse {

    private AnalysisProfileResponse currentUser;
    private AnalysisProfileResponse selectedAlumnus;
    private List<GapItemResponse> gapItems;
    private FieldCommentsResponse fieldComments;
    private OverallCommentResponse overallComment;
    private String scoreDescription;
    private String summarized;
    private String summary;
}
