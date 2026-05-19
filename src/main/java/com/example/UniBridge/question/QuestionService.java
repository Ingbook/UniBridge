package com.example.UniBridge.question;

import com.example.UniBridge.answer.AnswerDto;
import com.example.UniBridge.answer.AnswerRepository;
import com.example.UniBridge.company.Company;
import com.example.UniBridge.company.CompanyService;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

@Service
@RequiredArgsConstructor
public class QuestionService {

    private static final String STUDENT_WRITER_NAME = "익명 학생";

    private final QuestionRepository questionRepository;
    private final AnswerRepository answerRepository;
    private final CompanyService companyService;

    @Transactional(readOnly = true)
    public List<QuestionDto> getQuestions(String keyword, String category, Long companyId) {
        return questionRepository.findAll().stream()
                .filter(question -> !StringUtils.hasText(keyword)
                        || question.getTitle().contains(keyword)
                        || question.getContent().contains(keyword)
                        || question.getCompany().getName().contains(keyword))
                .filter(question -> !StringUtils.hasText(category) || question.getCategory().equalsIgnoreCase(category))
                .filter(question -> companyId == null || question.getCompany().getId().equals(companyId))
                .map(QuestionDto::from)
                .toList();
    }

    @Transactional
    public QuestionDetailDto getQuestion(Long questionId) {
        Question question = getQuestionEntity(questionId);
        question.increaseViewCount();
        List<AnswerDto> answers = answerRepository.findByQuestionIdOrderByAcceptedDescIdAsc(questionId).stream()
                .map(AnswerDto::from)
                .toList();
        return QuestionDetailDto.from(question, answers);
    }

    @Transactional
    public QuestionDto createQuestion(QuestionRequest request) {
        validate(request);
        Company company = companyService.getCompanyEntity(request.getCompanyId());
        Question question = Question.builder()
                .company(company)
                .category(request.getCategory())
                .title(request.getTitle())
                .content(request.getContent())
                .writerName(STUDENT_WRITER_NAME)
                .build();
        return QuestionDto.from(questionRepository.save(question));
    }

    @Transactional
    public QuestionDto updateQuestion(Long questionId, QuestionRequest request) {
        validate(request);
        Question question = getQuestionEntity(questionId);
        Company company = companyService.getCompanyEntity(request.getCompanyId());
        question.update(company, request.getCategory(), request.getTitle(), request.getContent());
        return QuestionDto.from(question);
    }

    @Transactional
    public void deleteQuestion(Long questionId) {
        Question question = getQuestionEntity(questionId);
        answerRepository.deleteByQuestionId(questionId);
        questionRepository.delete(question);
    }

    @Transactional(readOnly = true)
    public Question getQuestionEntity(Long questionId) {
        return questionRepository.findById(questionId)
                .orElseThrow(() -> new IllegalArgumentException("존재하지 않는 질문입니다."));
    }

    private void validate(QuestionRequest request) {
        if (request.getCompanyId() == null) {
            throw new IllegalArgumentException("기업 ID를 입력해 주세요.");
        }
        if (!StringUtils.hasText(request.getCategory())) {
            throw new IllegalArgumentException("질문 카테고리를 입력해 주세요.");
        }
        if (!StringUtils.hasText(request.getTitle())) {
            throw new IllegalArgumentException("질문 제목을 입력해 주세요.");
        }
        if (!StringUtils.hasText(request.getContent())) {
            throw new IllegalArgumentException("질문 내용을 입력해 주세요.");
        }
    }
}
