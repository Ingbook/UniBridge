package com.example.UniBridge.common;

import com.example.UniBridge.answer.Answer;
import com.example.UniBridge.answer.AnswerRepository;
import com.example.UniBridge.company.Company;
import com.example.UniBridge.company.CompanyRepository;
import com.example.UniBridge.question.Question;
import com.example.UniBridge.question.QuestionRepository;
import com.example.UniBridge.specification.entity.Specification;
import com.example.UniBridge.specification.repository.SpecificationRepository;
import java.math.BigDecimal;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.CommandLineRunner;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
@Order(Ordered.HIGHEST_PRECEDENCE + 1)
@RequiredArgsConstructor
public class MockDataInitializer implements CommandLineRunner {

    private final CompanyRepository companyRepository;
    private final SpecificationRepository specificationRepository;
    private final QuestionRepository questionRepository;
    private final AnswerRepository answerRepository;

    @Override
    @Transactional
    public void run(String... args) {
        specificationRepository.findByUserId(1L)
                .orElseGet(() -> specificationRepository.save(Specification.builder()
                        .userId(1L)
                        .gpa(BigDecimal.valueOf(3.8))
                        .maxGpa(BigDecimal.valueOf(4.5))
                        .build()));

        if (questionRepository.count() > 0) {
            return;
        }

        List<Company> companies = companyRepository.findAll(Sort.by("id"));
        if (companies.size() < 3) {
            return;
        }

        Company techCorp = companies.get(0);
        Company dataFlow = companies.get(1);
        Company secureApp = companies.get(2);

        Question question = questionRepository.save(Question.builder()
                .company(techCorp)
                .category("Backend Developer")
                .title("백엔드 포트폴리오에 어떤 기능까지 넣어야 하나요?")
                .content("Spring Boot 프로젝트를 진행 중인데 궁금합니다.")
                .writerName("익명 학생")
                .build());
        questionRepository.save(Question.builder()
                .company(dataFlow)
                .category("Interview")
                .title("기술 면접에서 JPA 질문은 어느 정도까지 준비해야 하나요?")
                .content("연관관계와 트랜잭션 중심으로 준비하면 충분한지 궁금합니다.")
                .writerName("익명 학생")
                .build());
        questionRepository.save(Question.builder()
                .company(secureApp)
                .category("Resume")
                .title("소프트웨어 직무 자기소개서에서 프로젝트 경험을 어떻게 써야 하나요?")
                .content("팀 프로젝트 경험을 직무 역량과 연결하는 방법이 궁금합니다.")
                .writerName("익명 학생")
                .build());

        answerRepository.save(Answer.builder()
                .question(question)
                .writerName("익명 현직자")
                .content("인증, 예외 처리, 테스트 코드, 배포 경험이 보이면 백엔드 기본기가 잘 드러납니다.")
                .accepted(false)
                .build());
    }
}
