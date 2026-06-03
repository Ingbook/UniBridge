package com.example.UniBridge.analysis.service;

import com.example.UniBridge.analysis.dto.ComparisonItemResponse;
import com.example.UniBridge.analysis.dto.ComparisonStatus;
import com.example.UniBridge.analysis.dto.SpecProfileResponse;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Arrays;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import org.springframework.stereotype.Component;

@Component
public class AnalysisScoreCalculator {

    public static final String DESCRIPTION_FALLBACK = "설명 보완 필요";

    private static final List<CategoryWeight> WEIGHTS = List.of(
            new CategoryWeight("GPA", "학점", 30),
            new CategoryWeight("LANGUAGE", "어학성적", 15),
            new CategoryWeight("CERTIFICATION", "자격증", 20),
            new CategoryWeight("AWARD", "수상경력", 10),
            new CategoryWeight("PROJECT", "프로젝트", 15),
            new CategoryWeight("PORTFOLIO", "포트폴리오", 10)
    );
    private static final List<String> TEXT_KEYWORDS = List.of(
            "AI", "데이터", "개발", "서비스", "배포", "운영", "프로젝트", "분석",
            "Spring", "React", "Python", "TensorFlow", "모델", "API", "DB", "클라우드"
    );

    public AnalysisScoreResult calculate(SpecProfileResponse userProfile, SpecProfileResponse alumnusProfile) {
        List<ComparisonItemResponse> gapItems = WEIGHTS.stream()
                .map(weight -> createGapItem(weight, userProfile, alumnusProfile))
                .toList();
        int overallScore = clamp((int) Math.round(gapItems.stream()
                .mapToDouble(item -> item.getScore() * weightFor(item.getCategory()) / 100.0)
                .sum()));
        return new AnalysisScoreResult(
                overallScore,
                createScoreDescription(overallScore, alumnusProfile.getName()),
                createSummarized(overallScore, gapItems, userProfile),
                gapItems
        );
    }

    public List<String> validCertificationNames(List<String> names) {
        if (names == null || names.isEmpty()) {
            return List.of();
        }
        return names.stream()
                .filter(this::hasText)
                .flatMap(name -> Arrays.stream(name.split("[,/]")))
                .map(String::trim)
                .filter(this::hasText)
                .distinct()
                .toList();
    }

    public List<String> validCertificationNamesFromText(String names) {
        if (!hasText(names)) {
            return List.of();
        }
        return validCertificationNames(List.of(names));
    }

    public String normalizeDescription(String description) {
        if (!hasText(description) || description.trim().length() < 10) {
            return DESCRIPTION_FALLBACK;
        }
        return description.trim();
    }

    private ComparisonItemResponse createGapItem(CategoryWeight weight, SpecProfileResponse userProfile,
                                                 SpecProfileResponse alumnusProfile) {
        ComparisonDecision decision = compare(weight.category(), userProfile, alumnusProfile);
        String userValue = valueFor(weight.category(), userProfile);
        String alumnusValue = valueFor(weight.category(), alumnusProfile);

        return ComparisonItemResponse.builder()
                .category(weight.category())
                .displayName(weight.displayName())
                .userValue(userValue)
                .alumnusValue(alumnusValue)
                .score(decision.score())
                .aiScore(decision.score())
                .status(decision.status())
                .comment(decision.comment())
                .gapDescription(decision.comment())
                .build();
    }

    private ComparisonDecision compare(String category, SpecProfileResponse userProfile,
                                       SpecProfileResponse alumnusProfile) {
        double userValue = numericValue(category, userProfile);
        double alumnusValue = numericValue(category, alumnusProfile);
        int score = achievementScore(userValue, alumnusValue);
        boolean competitive = userValue >= alumnusValue;
        String status = competitive
                ? ComparisonStatus.EXCELLENT.name()
                : ComparisonStatus.NEEDS_IMPROVEMENT.name();
        String comment = competitive
                ? competitiveComment(category, userValue, alumnusValue)
                : improvementComment(category, userValue, alumnusValue);
        return new ComparisonDecision(score, status, comment);
    }

    private double numericValue(String category, SpecProfileResponse profile) {
        return switch (category) {
            case "GPA" -> gpaOnMaxScale(profile);
            case "LANGUAGE" -> profile.getLanguageScore() == null ? 0 : Math.max(profile.getLanguageScore(), 0);
            case "CERTIFICATION" -> validCertificationNames(profile.getCertificationNames()).size();
            case "AWARD" -> safeCount(profile.getAwardCount());
            case "PROJECT" -> textMetric(profile.getProjectSummary());
            case "PORTFOLIO" -> textMetric(profile.getPortfolioDescription());
            default -> 0;
        };
    }

    private int achievementScore(double userValue, double alumnusValue) {
        if (alumnusValue <= 0) {
            return userValue <= 0 ? 100 : 100;
        }
        return clamp((int) Math.round((userValue / alumnusValue) * 100));
    }

    private double textMetric(String text) {
        if (DESCRIPTION_FALLBACK.equals(normalizeDescription(text))) {
            return 0;
        }
        return textScore(text);
    }

    private int textScore(String userText) {
        if (DESCRIPTION_FALLBACK.equals(normalizeDescription(userText))) {
            return 0;
        }
        String normalizedUser = userText.trim();
        int lengthScore = Math.min(normalizedUser.length() * 2, 60);
        int keywordScore = (int) TEXT_KEYWORDS.stream()
                .filter(keyword -> containsIgnoreCase(normalizedUser, keyword))
                .count() * 6;
        return clamp(lengthScore + Math.min(keywordScore, 40));
    }

    private String valueFor(String category, SpecProfileResponse profile) {
        return switch (category) {
            case "GPA" -> "%s/%s".formatted(defaultNumber(profile.getGpa()), defaultNumber(profile.getMaxGpa()));
            case "LANGUAGE" -> "%s %s".formatted(defaultText(profile.getLanguageType(), "TOEIC"),
                    profile.getLanguageScore() == null ? "-" : profile.getLanguageScore());
            case "CERTIFICATION" -> formatCertificationValue(validCertificationNames(profile.getCertificationNames()));
            case "AWARD" -> "%d개".formatted(safeCount(profile.getAwardCount()));
            case "PROJECT" -> normalizeDescription(profile.getProjectSummary());
            case "PORTFOLIO" -> normalizeDescription(profile.getPortfolioDescription());
            default -> "";
        };
    }

    private String formatCertificationValue(List<String> names) {
        if (names == null || names.isEmpty()) {
            return "0개";
        }
        int displayLimit = 3;
        String displayedNames = String.join(", ", names.stream()
                .limit(displayLimit)
                .toList());
        int remainingCount = Math.max(names.size() - displayLimit, 0);
        if (remainingCount > 0) {
            displayedNames += " 외 %d개".formatted(remainingCount);
        }
        return "%s / 총 %d개".formatted(displayedNames, names.size());
    }

    private String competitiveComment(String category, double userValue, double alumnusValue) {
        if ("CERTIFICATION".equals(category) && userValue > alumnusValue) {
            return "사용자가 선택 동문보다 자격증을 더 많이 보유하고 있습니다.";
        }
        if ("CERTIFICATION".equals(category) && userValue == alumnusValue) {
            return "선택 동문과 비슷한 수준의 자격증을 보유하고 있습니다.";
        }
        return "선택 동문 기준과 비교해 충분히 경쟁력 있는 항목입니다.";
    }

    private String improvementComment(String category, double userValue, double alumnusValue) {
        double gap = Math.max(alumnusValue - userValue, 0);
        return switch (category) {
            case "GPA" -> "선택 동문보다 %.1f점 낮습니다.".formatted(gap);
            case "LANGUAGE" -> "선택 동문보다 %d점 낮습니다.".formatted(Math.round(gap));
            case "CERTIFICATION" -> "선택 동문보다 자격증 수가 부족해 보완이 필요합니다.";
            case "AWARD" -> "선택 동문보다 수상경력이 %d개 부족해 보완이 필요합니다.".formatted(Math.round(gap));
            case "PROJECT" -> "선택 동문보다 프로젝트 설명 경쟁력이 낮아 보완이 필요한 항목입니다.";
            case "PORTFOLIO" -> "선택 동문보다 포트폴리오 설명 경쟁력이 낮아 보완이 필요한 항목입니다.";
            default -> "선택 동문보다 낮아 보완이 필요한 항목입니다.";
        };
    }

    private double gpaOnMaxScale(SpecProfileResponse profile) {
        double normalized = normalizedGpa(profile);
        double max = profile.getMaxGpa() == null ? 4.5 : profile.getMaxGpa().doubleValue();
        if (max <= 0) {
            max = 4.5;
        }
        return normalized * max;
    }

    private String createScoreDescription(int overallScore, String alumnusName) {
        String targetName = hasText(alumnusName) ? alumnusName.trim() : "선택 동문";
        if (overallScore >= 85) {
            return "%s 기준 준비도가 높습니다.".formatted(targetName);
        }
        if (overallScore >= 70) {
            return "%s 기준 준비도가 양호하지만 일부 항목 보완이 필요합니다.".formatted(targetName);
        }
        return "%s 기준 핵심 스펙 보완이 필요합니다.".formatted(targetName);
    }

    private String createSummarized(int overallScore, List<ComparisonItemResponse> gapItems,
                                    SpecProfileResponse userProfile) {
        String weakest = gapItems.stream()
                .min((left, right) -> Integer.compare(left.getScore(), right.getScore()))
                .map(ComparisonItemResponse::getDisplayName)
                .orElse("핵심 항목");
        String project = normalizeDescription(userProfile.getProjectSummary());
        if (!DESCRIPTION_FALLBACK.equals(project)) {
            String shortProject = project.length() > 40 ? project.substring(0, 40) + "..." : project;
            return "사용자의 '%s' 경험을 기반으로 %d점으로 산출했습니다. 가장 우선 보완할 항목은 %s입니다."
                    .formatted(shortProject, overallScore, weakest);
        }
        return "프로젝트 설명 보완 필요 상태라 %d점으로 산출했습니다. 가장 우선 보완할 항목은 %s입니다."
                .formatted(overallScore, weakest);
    }

    private double normalizedGpa(SpecProfileResponse profile) {
        if (profile.getGpa() == null || profile.getMaxGpa() == null
                || BigDecimal.ZERO.compareTo(profile.getMaxGpa()) >= 0) {
            return 0;
        }
        return profile.getGpa()
                .divide(profile.getMaxGpa(), 4, RoundingMode.HALF_UP)
                .doubleValue();
    }

    private int weightFor(String category) {
        return WEIGHTS.stream()
                .filter(weight -> weight.category().equals(category))
                .findFirst()
                .map(CategoryWeight::weight)
                .orElse(0);
    }

    private boolean containsIgnoreCase(String text, String keyword) {
        return text.toLowerCase(Locale.ROOT).contains(keyword.toLowerCase(Locale.ROOT));
    }

    private String defaultNumber(BigDecimal value) {
        return value == null ? "-" : value.stripTrailingZeros().toPlainString();
    }

    private String defaultText(String value, String defaultValue) {
        return hasText(value) ? value.trim() : defaultValue;
    }

    private boolean hasText(String value) {
        return value != null && !value.isBlank();
    }

    private int safeCount(Integer count) {
        return count == null ? 0 : Math.max(count, 0);
    }

    private int clamp(int value) {
        return Math.max(0, Math.min(100, value));
    }

    public record AnalysisScoreResult(
            int overallScore,
            String scoreDescription,
            String summarized,
            List<ComparisonItemResponse> gapItems
    ) {
    }

    private record CategoryWeight(String category, String displayName, int weight) {
    }

    private record ComparisonDecision(int score, String status, String comment) {
    }
}
