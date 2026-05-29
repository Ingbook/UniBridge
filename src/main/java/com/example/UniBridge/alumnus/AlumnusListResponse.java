package com.example.UniBridge.alumnus;

import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class AlumnusListResponse {

    private Long id;
    private String name;
    private String jobRole;
    private String profileImageUrl;
    private Integer representativeScore;
    private String specSummary;

    public static AlumnusListResponse from(Alumnus alumnus) {
        return AlumnusListResponse.builder()
                .id(alumnus.getId())
                .name(alumnus.getName())
                .jobRole(alumnus.getJobRole())
                .profileImageUrl(alumnus.getProfileImageUrl())
                .representativeScore(alumnus.getRepresentativeScore())
                .specSummary("%s %s, 자격증 %d개".formatted(
                        blankToDefault(alumnus.getLanguageType(), "어학"),
                        alumnus.getLanguageScore() == null ? "-" : alumnus.getLanguageScore(),
                        alumnus.getCertificationCount() == null ? 0 : alumnus.getCertificationCount()))
                .build();
    }

    private static String blankToDefault(String value, String defaultValue) {
        return value == null || value.isBlank() ? defaultValue : value;
    }
}
