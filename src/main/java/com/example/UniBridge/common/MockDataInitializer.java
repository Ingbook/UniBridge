현package com.example.UniBridge.common;

import com.example.UniBridge.answer.Answer;
import com.example.UniBridge.answer.AnswerRepository;
import com.example.UniBridge.company.Company;
import com.example.UniBridge.company.CompanyRepository;
import com.example.UniBridge.question.Question;
import com.example.UniBridge.question.QuestionRepository;
import com.example.UniBridge.specification.Specification;
import com.example.UniBridge.specification.SpecificationRepository;
import java.math.BigDecimal;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
@RequiredArgsConstructor
public class MockDataInitializer implements CommandLineRunner {

    private final CompanyRepository companyRepository;
    private final SpecificationRepository specificationRepository;
    private final QuestionRepository questionRepository;
    private final AnswerRepository answerRepository;

    @Override
    @Transactional
    public void run(String... args) {
        if (companyRepository.count() > 0) {
            return;
        }

        Company naver = companyRepository.save(Company.builder()
                .name("네이버")
                .industry("IT")
                .mainJobRole("BACKEND")
                .averageScore(86)
                .build());
        Company kakao = companyRepository.save(Company.builder()
                .name("카카오")
                .industry("IT")
                .mainJobRole("BACKEND")
                .averageScore(84)
                .build());
        Company samsung = companyRepository.save(Company.builder()
                .name("삼성전자")
                .industry("MANUFACTURING")
                .mainJobRole("SOFTWARE")
                .averageScore(82)
                .build());
        companyRepository.save(Company.builder()
                .name("토스")
                .industry("FINTECH")
                .mainJobRole("BACKEND")
                .averageScore(88)
                .build());
        companyRepository.save(Company.builder()
                .name("현대오토에버")
                .industry("MOBILITY")
                .mainJobRole("DATA")
                .averageScore(78)
                .build());
        companyRepository.save(Company.builder()
                .name("쿠팡")
                .industry("COMMERCE")
                .mainJobRole("BACKEND")
                .averageScore(81)
                .build());

        specificationRepository.save(Specification.builder()
                .userId(1L)
                .gpa(BigDecimal.valueOf(3.8))
                .maxGpa(BigDecimal.valueOf(4.5))
                .languageType("TOEIC")
                .languageScore(820)
                .certifications("정보처리기사, SQLD")
                .awards("교내 해커톤 우수상")
                .projects("Spring Boot, Java, JPA, DB, REST API, 배포 경험이 포함된 커리어 분석 프로젝트")
                .internships("")
                .portfolioUrl("https://github.com/example/unibridge")
                .build());

        Question question = questionRepository.save(Question.builder()
                .company(naver)
                .category("BACKEND")
                .title("백엔드 포트폴리오에 어떤 기능까지 넣어야 하나요?")
                .content("Spring Boot 프로젝트를 진행 중인데 궁금합니다.")
                .writerName("익명 학생")
                .build());
        questionRepository.save(Question.builder()
                .company(kakao)
                .category("INTERVIEW")
                .title("기술 면접에서 JPA 질문은 어느 정도까지 준비해야 하나요?")
                .content("연관관계와 트랜잭션 중심으로 준비하면 충분한지 궁금합니다.")
                .writerName("익명 학생")
                .build());
        questionRepository.save(Question.builder()
                .company(samsung)
                .category("RESUME")
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
