package com.example.UniBridge.controller;

import com.example.UniBridge.answer.AnswerDto;
import com.example.UniBridge.answer.AnswerRequest;
import com.example.UniBridge.answer.AnswerService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;

@Controller
@RequiredArgsConstructor
public class AnswerWebController {

    private final AnswerService answerService;

    @PostMapping("/question/{questionId}/answer")
    public String createAnswer(
            @PathVariable Long questionId,
            @RequestParam("content") String content
    ) {
        AnswerRequest request = new AnswerRequest(content);
        answerService.createAnswer(questionId, request);
        return "redirect:/question/" + questionId;
    }

    @PostMapping("/answer/{answerId}/accept")
    public String acceptAnswer(@PathVariable Long answerId) {
        AnswerDto answerDto = answerService.acceptAnswer(answerId);
        return "redirect:/question/" + answerDto.getQuestionId();
    }
}
