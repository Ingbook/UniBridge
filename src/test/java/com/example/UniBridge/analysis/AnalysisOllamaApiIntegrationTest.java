package com.example.UniBridge.analysis;

import static org.hamcrest.Matchers.allOf;
import static org.hamcrest.Matchers.greaterThanOrEqualTo;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.lessThanOrEqualTo;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.example.UniBridge.certification.entity.Certification;
import com.example.UniBridge.certification.entity.UserCertification;
import com.example.UniBridge.certification.repository.CertificationRepository;
import com.example.UniBridge.certification.repository.UserCertificationRepository;
import com.example.UniBridge.company.Company;
import com.example.UniBridge.company.CompanyRepository;
import com.example.UniBridge.specification.entity.Specification;
import com.example.UniBridge.specification.repository.SpecificationRepository;
import java.math.BigDecimal;
import java.time.LocalDate;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

@Tag("ollama")
@SpringBootTest(properties = {
        "unibridge.ai.enabled=true",
        "spring.ai.ollama.base-url=${OLLAMA_BASE_URL:http://localhost:11434}",
        "spring.ai.ollama.chat.options.model=${OLLAMA_MODEL:gemma3:1b}",
        "spring.ai.ollama.chat.options.temperature=0"
})
@AutoConfigureMockMvc
class AnalysisOllamaApiIntegrationTest {

    private static final Long CURRENT_USER_ID = 1L;

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private CompanyRepository companyRepository;

    @Autowired
    private SpecificationRepository specificationRepository;

    @Autowired
    private CertificationRepository certificationRepository;

    @Autowired
    private UserCertificationRepository userCertificationRepository;

    private Company company;

    @BeforeEach
    void setUp() {
        company = companyRepository.save(Company.builder()
                .name("Ollama테스트기업")
                .industry("IT 서비스")
                .mainJobRole("Backend Developer")
                .averageScore(85)
                .build());

        Specification specification = specificationRepository.findByUserId(CURRENT_USER_ID)
                .orElseGet(() -> Specification.builder().userId(CURRENT_USER_ID).build());
        specification.update(BigDecimal.valueOf(3.8), BigDecimal.valueOf(4.5));
        specificationRepository.save(specification);

        Certification certification = certificationRepository.findByName("Ollama테스트자격증")
                .orElseGet(() -> certificationRepository.save(Certification.builder()
                        .name("Ollama테스트자격증")
                        .category("BACKEND")
                        .score(30)
                        .description("Ollama API 통합 테스트용 자격증")
                        .build()));

        userCertificationRepository.deleteByUserIdAndCertificationId(CURRENT_USER_ID, certification.getId());
        userCertificationRepository.save(UserCertification.builder()
                .userId(CURRENT_USER_ID)
                .certification(certification)
                .acquiredDate(LocalDate.of(2025, 1, 10))
                .build());
    }

    @Test
    void analyzeGpaAndCertification_usesRealOllamaAnalysis() throws Exception {
        String request = """
                {
                  "companyId": %d
                }
                """.formatted(company.getId());

        mockMvc.perform(post("/api/analysis/gpa-certification")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(request))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success", is(true)))
                .andExpect(jsonPath("$.data.companyId", is(company.getId().intValue())))
                .andExpect(jsonPath("$.data.gpaScore", is(84)))
                .andExpect(jsonPath("$.data.certificationScore", is(30)))
                .andExpect(jsonPath("$.data.totalScore", is(62)))
                .andExpect(jsonPath("$.data.aiAnalysisSource", is("OLLAMA")))
                .andExpect(jsonPath("$.data.aiAdjustmentScore",
                        allOf(greaterThanOrEqualTo(-10), lessThanOrEqualTo(10))))
                .andExpect(jsonPath("$.data.aiAdjustedScore",
                        allOf(greaterThanOrEqualTo(0), lessThanOrEqualTo(100))))
                .andExpect(jsonPath("$.data.aiSummary").isNotEmpty())
                .andExpect(jsonPath("$.data.aiRecommendation").isNotEmpty())
                .andExpect(jsonPath("$.data.summary").isNotEmpty());
    }
}
