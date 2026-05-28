package com.example.UniBridge.controller;

import com.example.UniBridge.company.CompanyDto;
import com.example.UniBridge.company.CompanyService;
import com.example.UniBridge.question.QuestionDto;
import com.example.UniBridge.question.QuestionService;
import java.util.Comparator;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;

@Controller
@RequiredArgsConstructor
public class BoardController {

    private static final String POPULAR_SORT = "popular";

    private final CompanyService companyService;
    private final QuestionService questionService;

    @GetMapping({"/board", "/board/{companyId}"})
    public String board(
            @PathVariable(required = false) Long companyId,
            @RequestParam(required = false, defaultValue = "recent") String sort,
            Model model
    ) {
        boolean popular = POPULAR_SORT.equalsIgnoreCase(sort);
        List<CompanyDto> companies = companyService.getCompanies(null, null, null).stream()
                .sorted(Comparator.comparing(CompanyDto::getName, String.CASE_INSENSITIVE_ORDER))
                .toList();
        List<QuestionDto> questions = questionService.getQuestions(null, null, companyId).stream()
                .sorted(popular
                        ? Comparator.comparing(QuestionDto::getViewCount, Comparator.nullsLast(Comparator.reverseOrder()))
                                .thenComparing(QuestionDto::getId, Comparator.reverseOrder())
                        : Comparator.comparing(QuestionDto::getId, Comparator.reverseOrder()))
                .toList();

        model.addAttribute("companies", companies);
        model.addAttribute("questions", questions);
        model.addAttribute("activeCompanyId", companyId);
        model.addAttribute("activeCompanyName", findCompanyName(companies, companyId));
        model.addAttribute("popular", popular);

        return "board";
    }

    private String findCompanyName(List<CompanyDto> companies, Long companyId) {
        if (companyId == null) {
            return null;
        }
        return companies.stream()
                .filter(company -> company.getId().equals(companyId))
                .map(CompanyDto::getName)
                .findFirst()
                .orElse("Company");
    }
}
