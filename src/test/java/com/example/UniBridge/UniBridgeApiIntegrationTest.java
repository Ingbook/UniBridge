package com.example.UniBridge;

import static org.hamcrest.Matchers.greaterThanOrEqualTo;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.example.UniBridge.company.Company;
import com.example.UniBridge.company.CompanyRepository;
import com.example.UniBridge.specification.Specification;
import com.example.UniBridge.specification.SpecificationRepository;
import java.math.BigDecimal;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

@SpringBootTest
@AutoConfigureMockMvc
class UniBridgeApiIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private CompanyRepository companyRepository;

    @Autowired
    private SpecificationRepository specificationRepository;

    @BeforeEach
    void setUp() {
        if (companyRepository.count() == 0) {
            companyRepository.save(Company.builder()
                    .name("테스트기업")
                    .industry("IT")
                    .mainJobRole("BACKEND")
                    .averageScore(80)
                    .build());
        }
        restoreSpecification();
    }

    @Test
    void getCompanies_returnsBaseResponseAndSupportsFilters() throws Exception {
        mockMvc.perform(get("/api/companies")
                        .param("keyword", "네이버")
                        .param("industry", "IT")
                        .param("jobRole", "BACKEND"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success", is(true)))
                .andExpect(jsonPath("$.message", is("요청 성공")))
                .andExpect(jsonPath("$.data", hasSize(greaterThanOrEqualTo(1))))
                .andExpect(jsonPath("$.data[0].name", is("네이버")));
    }

    @Test
    void getCompany_whenCompanyDoesNotExist_returnsFailResponse() throws Exception {
        mockMvc.perform(get("/api/companies/999999"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success", is(false)))
                .andExpect(jsonPath("$.message", is("존재하지 않는 기업입니다.")))
                .andExpect(jsonPath("$.data").doesNotExist());
    }

    @Test
    void getPopularCompanies_returnsTopFiveByAverageScore() throws Exception {
        mockMvc.perform(get("/api/companies/popular"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success", is(true)))
                .andExpect(jsonPath("$.data", hasSize(5)))
                .andExpect(jsonPath("$.data[0].averageScore", is(88)));
    }

    @Test
    void upsertAndGetMySpecification() throws Exception {
        String request = """
                {
                  "gpa": 4.1,
                  "maxGpa": 4.5,
                  "languageType": "TOEIC",
                  "languageScore": 900,
                  "certifications": "정보처리기사, SQLD, ADsP",
                  "awards": "해커톤 대상",
                  "projects": "Spring Boot Java JPA DB API 배포 프로젝트",
                  "internships": "백엔드 인턴",
                  "portfolioUrl": "https://github.com/example/portfolio"
                }
                """;

        mockMvc.perform(put("/api/specifications/me")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(request))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success", is(true)))
                .andExpect(jsonPath("$.data.userId", is(1)))
                .andExpect(jsonPath("$.data.gpa", is(4.1)))
                .andExpect(jsonPath("$.data.languageScore", is(900)));

        mockMvc.perform(get("/api/specifications/me"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success", is(true)))
                .andExpect(jsonPath("$.data.portfolioUrl", is("https://github.com/example/portfolio")));
    }

    @Test
    void deleteMySpecification_removesSpecificationAndMissingGetFails() throws Exception {
        mockMvc.perform(delete("/api/specifications/me"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success", is(true)));

        mockMvc.perform(get("/api/specifications/me"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success", is(false)))
                .andExpect(jsonPath("$.message", is("등록된 스펙 정보가 없습니다.")));

        restoreSpecification();
    }

    @Test
    void analyzeGap_calculatesWeightedScoreAndStoresReport() throws Exception {
        mockMvc.perform(post("/api/analysis/gap")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"companyId\":1}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success", is(true)))
                .andExpect(jsonPath("$.data.companyId", is(1)))
                .andExpect(jsonPath("$.data.companyName", is("네이버")))
                .andExpect(jsonPath("$.data.userScore", is(79)))
                .andExpect(jsonPath("$.data.gapScore", is(7)))
                .andExpect(jsonPath("$.data.gpaScore", is(84)))
                .andExpect(jsonPath("$.data.languageScore", is(83)))
                .andExpect(jsonPath("$.data.certificationScore", is(40)))
                .andExpect(jsonPath("$.data.projectScore", is(100)))
                .andExpect(jsonPath("$.data.activityScore", is(80)));

        mockMvc.perform(get("/api/analysis/me"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success", is(true)))
                .andExpect(jsonPath("$.data", hasSize(greaterThanOrEqualTo(1))));
    }

    @Test
    void analyzeGap_withoutSpecification_returnsKoreanErrorMessage() throws Exception {
        specificationRepository.findByUserId(1L).ifPresent(specificationRepository::delete);

        mockMvc.perform(post("/api/analysis/gap")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"companyId\":1}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success", is(false)))
                .andExpect(jsonPath("$.message", is("AI 분석을 진행하려면 먼저 스펙 정보를 등록해야 합니다.")));

        restoreSpecification();
    }

    @Test
    void questionCrudDetailAndAnswerAcceptFlow() throws Exception {
        String createQuestionRequest = """
                {
                  "companyId": 1,
                  "category": "BACKEND",
                  "title": "테스트 질문입니다",
                  "content": "Spring Boot 포트폴리오 질문입니다."
                }
                """;

        String questionId = mockMvc.perform(post("/api/questions")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(createQuestionRequest))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success", is(true)))
                .andExpect(jsonPath("$.data.writerName", is("익명 학생")))
                .andReturn()
                .getResponse()
                .getContentAsString()
                .replaceAll(".*\"id\":(\\d+).*", "$1");

        mockMvc.perform(get("/api/questions/{questionId}", questionId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success", is(true)))
                .andExpect(jsonPath("$.data.viewCount", is(1)))
                .andExpect(jsonPath("$.data.answers", hasSize(0)));

        String updateQuestionRequest = """
                {
                  "companyId": 1,
                  "category": "BACKEND",
                  "title": "수정된 테스트 질문입니다",
                  "content": "수정된 질문 내용입니다."
                }
                """;

        mockMvc.perform(put("/api/questions/{questionId}", questionId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(updateQuestionRequest))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.title", is("수정된 테스트 질문입니다")));

        String answerId = mockMvc.perform(post("/api/questions/{questionId}/answers", questionId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"content\":\"인증, 예외 처리, 테스트 코드가 중요합니다.\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.writerName", is("익명 현직자")))
                .andExpect(jsonPath("$.data.accepted", is(false)))
                .andReturn()
                .getResponse()
                .getContentAsString()
                .replaceAll(".*\"id\":(\\d+).*", "$1");

        mockMvc.perform(patch("/api/answers/{answerId}/accept", answerId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.accepted", is(true)));

        mockMvc.perform(get("/api/questions/{questionId}", questionId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.viewCount", is(2)))
                .andExpect(jsonPath("$.data.answers", hasSize(1)))
                .andExpect(jsonPath("$.data.answers[0].accepted", is(true)));

        mockMvc.perform(delete("/api/questions/{questionId}", questionId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success", is(true)));
    }

    private void restoreSpecification() {
        Specification specification = specificationRepository.findByUserId(1L)
                .orElseGet(() -> Specification.builder().userId(1L).build());
        specification.update(
                BigDecimal.valueOf(3.8),
                BigDecimal.valueOf(4.5),
                "TOEIC",
                820,
                "정보처리기사, SQLD",
                "교내 해커톤 우수상",
                "Spring Boot, Java, JPA, DB, REST API, 배포 경험이 포함된 커리어 분석 프로젝트",
                "",
                "https://github.com/example/unibridge"
        );
        specificationRepository.save(specification);
    }
}
