package com.example.UniBridge.analysis.dto;

import java.math.BigDecimal;
import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AiProfileAnalysisResponse {

    private UserProfile userProfile;
    private AiAnalysis aiAnalysis;

    @Getter
    @Setter
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class UserProfile {

        private String name;
        private BigDecimal gpa;
        private Language language;
        private Certifications certifications;
        private Integer awardCount;
        private String project;
        private String portfolio;
    }

    @Getter
    @Setter
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class Language {

        private String type;
        private Integer score;
        private String displayText;
    }

    @Getter
    @Setter
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class Certifications {

        private List<String> items;
        private Integer count;
    }

    @Getter
    @Setter
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class AiAnalysis {

        private List<String> strengths;
        private List<String> weaknesses;
        private String comment;
    }
}
