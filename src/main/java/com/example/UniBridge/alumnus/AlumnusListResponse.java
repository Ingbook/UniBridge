package com.example.UniBridge.alumnus;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Arrays;
import java.util.Locale;
import java.util.Set;
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
                .representativeScore(calculateRepresentativeScore(alumnus))
                .specSummary("%s %s, 자격증 %d개".formatted(
                        blankToDefault(alumnus.getLanguageType(), "어학"),
                        alumnus.getLanguageScore() == null ? "-" : alumnus.getLanguageScore(),
                        alumnus.getCertificationCount() == null ? 0 : alumnus.getCertificationCount()))
                .build();
    }

    private static String blankToDefault(String value, String defaultValue) {
        return value == null || value.isBlank() ? defaultValue : value;
    }

    private static int calculateRepresentativeScore(Alumnus alumnus) {
        int gpaScore = normalizedGpa(alumnus.getGpa(), alumnus.getMaxGpa());
        int languageScore = clamp((int) Math.round((safeNumber(alumnus.getLanguageScore()) / 990.0) * 100));
        int certificationScore = clamp(allowedCertificationCount(alumnus.getCertificationSummary()) * 17);
        int awardScore = clamp(safeNumber(alumnus.getAwardCount()) * 25);
        int projectScore = textScore(alumnus.getProjectSummary());
        int portfolioScore = textScore(alumnus.getPortfolioDescription());

        return clamp((int) Math.round(
                gpaScore * 0.30
                        + languageScore * 0.15
                        + certificationScore * 0.20
                        + awardScore * 0.10
                        + projectScore * 0.15
                        + portfolioScore * 0.10
        ));
    }

    private static int normalizedGpa(BigDecimal gpa, BigDecimal maxGpa) {
        if (gpa == null || maxGpa == null || BigDecimal.ZERO.compareTo(maxGpa) >= 0) {
            return 0;
        }
        return clamp((int) Math.round(gpa.divide(maxGpa, 4, RoundingMode.HALF_UP).doubleValue() * 100));
    }

    private static int allowedCertificationCount(String certificationSummary) {
        if (certificationSummary == null || certificationSummary.isBlank()) {
            return 0;
        }
        Set<String> allowed = Set.of("정보처리기사", "sqld", "adsp", "awscloudpractitioner", "리눅스마스터2급", "컴퓨터활용능력1급");
        return (int) Arrays.stream(certificationSummary.split("[,/]"))
                .map(String::trim)
                .map(AlumnusListResponse::normalizeCertificationKey)
                .filter(allowed::contains)
                .distinct()
                .count();
    }

    private static String normalizeCertificationKey(String value) {
        return value.replaceAll("\\s+", "").toLowerCase(Locale.ROOT);
    }

    private static int textScore(String text) {
        if (text == null || text.isBlank() || text.trim().length() < 10) {
            return 30;
        }
        return clamp(45 + text.trim().length());
    }

    private static int safeNumber(Integer value) {
        return value == null ? 0 : Math.max(value, 0);
    }

    private static int clamp(int value) {
        return Math.max(0, Math.min(100, value));
    }
}
