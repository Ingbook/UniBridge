package com.example.UniBridge.analysis.dto;

import java.util.List;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class OverallCommentResponse {

    private List<String> strengths;
    private List<String> weaknesses;
    private String aiComment;
}
