package com.example.UniBridge.answer;

import com.example.UniBridge.question.Question;
import com.example.UniBridge.question.QuestionService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

@Service
@RequiredArgsConstructor
public class AnswerService {

    private static final String WORKER_WRITER_NAME = "익명 현직자";

    private final AnswerRepository answerRepository;
    private final QuestionService questionService;

    @Transactional
    public AnswerDto createAnswer(Long questionId, AnswerRequest request) {
        if (!StringUtils.hasText(request.getContent())) {
            throw new IllegalArgumentException("답변 내용을 입력해 주세요.");
        }
        Question question = questionService.getQuestionEntity(questionId);
        Answer answer = Answer.builder()
                .question(question)
                .writerName(WORKER_WRITER_NAME)
                .content(request.getContent())
                .accepted(false)
                .build();
        return AnswerDto.from(answerRepository.save(answer));
    }

    @Transactional
    public AnswerDto acceptAnswer(Long answerId) {
        Answer answer = answerRepository.findById(answerId)
                .orElseThrow(() -> new IllegalArgumentException("존재하지 않는 답변입니다."));
        answerRepository.findByQuestionId(answer.getQuestion().getId())
                .forEach(Answer::unaccept);
        answer.accept();
        return AnswerDto.from(answer);
    }
}
