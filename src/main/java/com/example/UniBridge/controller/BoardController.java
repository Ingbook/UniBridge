package com.example.UniBridge.controller;

import com.example.UniBridge.answer.AnswerRequest;
import com.example.UniBridge.company.CompanyDto;
import com.example.UniBridge.company.CompanyService;
import com.example.UniBridge.question.QuestionDetailDto;
import com.example.UniBridge.question.QuestionDto;
import com.example.UniBridge.question.QuestionRequest;
import com.example.UniBridge.question.QuestionService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.util.Comparator;
import java.util.List;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;

@Controller
@RequiredArgsConstructor
public class BoardController {

    @Data
    public static class BoardForm {
        @NotBlank(message = "질문 제목을 입력해 주세요.")
        @Size(min = 2, message = "제목은 최소 2글자 이상이어야 합니다.")
        private String title;

        @NotBlank(message = "질문 내용을 입력해 주세요.")
        @Size(min = 10, message = "본문은 최소 10글자 이상이어야 합니다.")
        private String content;

        @NotNull(message = "커뮤니티를 선택해 주세요.")
        private Long companyId; // 0 will be treated as Random

        private String category = "General"; // Default category if none selected
    }

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
        List<QuestionDto> questions = questionService.getQuestions(null, null, companyId != null && companyId == 0 ? null : companyId).stream()
                .sorted(popular
                        ? Comparator.comparing(QuestionDto::getViewCount, Comparator.nullsLast(Comparator.reverseOrder()))
                                .thenComparing(QuestionDto::getId, Comparator.reverseOrder())
                        : Comparator.comparing(QuestionDto::getId, Comparator.reverseOrder()))
                .toList();

        model.addAttribute("companies", companies);
        model.addAttribute("questions", questions);
        model.addAttribute("activeCompanyId", companyId);
        
        String activeCompanyName = null;
        if (companyId != null && companyId == 0) {
            activeCompanyName = "Random";
        } else {
            activeCompanyName = findCompanyName(companies, companyId);
        }
        model.addAttribute("activeCompanyName", activeCompanyName);
        model.addAttribute("popular", popular);

        return "board";
    }

    @GetMapping("/board/ask")
    public String question(Model model) {
        model.addAttribute("boardForm", new BoardForm());
        model.addAttribute("companies", companyService.getCompanies(null, null, null));
        return "board_form";
    }

    @PostMapping("/board/ask")
    public String createQuestion(
            @Valid @ModelAttribute("boardForm") BoardForm form,
            BindingResult bindingResult,
            Model model
    ) {
        if (bindingResult.hasErrors()) {
            model.addAttribute("companies", companyService.getCompanies(null, null, null));
            return "board_form";
        }

        QuestionRequest request = new QuestionRequest(
                form.getCompanyId() == 0 ? null : form.getCompanyId(),
                form.getCategory(),
                form.getTitle(),
                form.getContent()
            );
        questionService.createQuestion(request);

        return "redirect:/board";
    }

    @GetMapping("/question/{questionId}")
    public String questionDetail(@PathVariable Long questionId, Model model) {
        QuestionDetailDto questionDetail = questionService.getQuestion(questionId);
        model.addAttribute("question", questionDetail);
        model.addAttribute("answerRequest", new AnswerRequest()); // For the answer form
        return "question_detail";
    }

    private String findCompanyName(List<CompanyDto> companies, Long companyId) {
        if (companyId == null || companyId == 0) {
            return null;
        }
        return companies.stream()
                .filter(company -> company.getId().equals(companyId))
                .map(CompanyDto::getName)
                .findFirst()
                .orElse("Company");
    }
}
