package com.example.UniBridge.analysis.dto;

import java.util.ArrayList;
import java.util.List;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
public class OllamaGapAnalysisResult {

    private Integer totalScore;
    private String scoreDescription;
    private String summary;
    private List<Item> items = new ArrayList<>();
    private List<String> strengths = new ArrayList<>();
    private List<String> weaknesses = new ArrayList<>();
    private List<String> comments = new ArrayList<>();

    @Getter
    @Setter
    @NoArgsConstructor
    public static class Item {

        private String category;
        private String userValue;
        private String alumnusValue;
        private String gapDescription;
        private Integer aiScore;
        private ComparisonStatus status;
    }
}
