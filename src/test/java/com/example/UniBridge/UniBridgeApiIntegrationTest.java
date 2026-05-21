package com.example.UniBridge;

import static org.hamcrest.Matchers.greaterThanOrEqualTo;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.example.UniBridge.certification.entity.Certification;
import com.example.UniBridge.certification.repository.CertificationRepository;
import com.example.UniBridge.certification.repository.UserCertificationRepository;
import com.example.UniBridge.company.Company;
import com.example.UniBridge.company.CompanyRepository;
import com.example.UniBridge.specification.entity.Specification;
import com.example.UniBridge.specification.repository.SpecificationRepository;
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

    @Autowired
    private CertificationRepository certificationRepository;

    @Autowired
    private UserCertificationRepository userCertificationRepository;

    @BeforeEach
    void setUp() {
        if (companyRepository.count() == 0) {
            companyRepository.save(Company.builder()
                    .name("테스트기업")
                    .industry("IT")
                    .mainJobRole("Backend Developer")
                    .averageScore(80)
                    .build());
        }
        restoreSpecification();
        certificationRepository.findByName("정보처리기사")
                .ifPresent(certification -> userCertificationRepository.deleteByUserIdAndCertificationId(1L, certification.getId()));
        certificationRepository.findByName("SQLD")
                .ifPresent(certification -> userCertificationRepository.deleteByUserIdAndCertificationId(1L, certification.getId()));
    }

    @Test
    void companyApis_workWithInitializedCompanies() throws Exception {
        mockMvc.perform(get("/api/companies")
                        .param("keyword", "TechCorp")
                        .param("industry", "IT")
                        .param("jobRole", "Backend Developer"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success", is(true)))
                .andExpect(jsonPath("$.message", is("요청 성공")))
                .andExpect(jsonPath("$.data", hasSize(greaterThanOrEqualTo(1))))
                .andExpect(jsonPath("$.data[0].name", is("TechCorp")));

        mockMvc.perform(get("/api/companies/popular"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success", is(true)))
                .andExpect(jsonPath("$.data", hasSize(5)))
                .andExpect(jsonPath("$.data[0].averageScore", is(92)));
    }

    @Test
    void saveAndGetMySpecification() throws Exception {
        String request = """
                {
                  "gpa": 4.1,
                  "maxGpa": 4.5
                }
                """;

        mockMvc.perform(put("/api/specifications/me")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(request))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success", is(true)))
                .andExpect(jsonPath("$.message", is("스펙 저장 성공")))
                .andExpect(jsonPath("$.data.userId", is(1)))
                .andExpect(jsonPath("$.data.gpa", is(4.1)))
                .andExpect(jsonPath("$.data.maxGpa", is(4.5)));

        mockMvc.perform(get("/api/specifications/me"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success", is(true)))
                .andExpect(jsonPath("$.data.gpa", is(4.1)))
                .andExpect(jsonPath("$.data.maxGpa", is(4.5)));
    }

    @Test
    void certificationAndAnalysisScenario() throws Exception {
        Certification engineer = certificationRepository.findByName("정보처리기사").orElseThrow();
        Certification sqld = certificationRepository.findByName("SQLD").orElseThrow();

        mockMvc.perform(get("/api/certifications"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success", is(true)))
                .andExpect(jsonPath("$.data", hasSize(10)));

        mockMvc.perform(get("/api/users/me/certifications"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success", is(true)));

        mockMvc.perform(post("/api/users/me/certifications")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"certificationId\":" + engineer.getId() + ",\"acquiredDate\":\"2025-01-10\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message", is("보유 자격증 등록 성공")))
                .andExpect(jsonPath("$.data.name", is("정보처리기사")));

        mockMvc.perform(post("/api/users/me/certifications")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"certificationId\":" + sqld.getId() + ",\"acquiredDate\":\"2025-02-10\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.name", is("SQLD")));

        mockMvc.perform(post("/api/analysis/gpa-certification")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"companyId\":1}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success", is(true)))
                .andExpect(jsonPath("$.message", is("학점 및 자격증 기반 분석 성공")))
                .andExpect(jsonPath("$.data.companyId", is(1)))
                .andExpect(jsonPath("$.data.companyName", is("TechCorp")))
                .andExpect(jsonPath("$.data.targetAverageScore", is(85)))
                .andExpect(jsonPath("$.data.gpaScore", is(84)))
                .andExpect(jsonPath("$.data.certificationScore", is(50)))
                .andExpect(jsonPath("$.data.totalScore", is(70)))
                .andExpect(jsonPath("$.data.gapScore", is(15)));

        mockMvc.perform(get("/api/analysis/me"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success", is(true)))
                .andExpect(jsonPath("$.data", hasSize(greaterThanOrEqualTo(1))));

        mockMvc.perform(delete("/api/users/me/certifications/{certificationId}", sqld.getId()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message", is("보유 자격증 삭제 성공")))
                .andExpect(jsonPath("$.data").doesNotExist());
    }

    @Test
    void analyzeWithoutSpecification_returnsKoreanErrorMessage() throws Exception {
        specificationRepository.findByUserId(1L).ifPresent(specificationRepository::delete);

        mockMvc.perform(post("/api/analysis/gpa-certification")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"companyId\":1}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success", is(false)))
                .andExpect(jsonPath("$.message", is("분석을 진행하려면 먼저 학점 정보를 등록해야 합니다.")));

        restoreSpecification();
    }

    private void restoreSpecification() {
        Specification specification = specificationRepository.findByUserId(1L)
                .orElseGet(() -> Specification.builder().userId(1L).build());
        specification.update(BigDecimal.valueOf(3.8), BigDecimal.valueOf(4.5));
        specificationRepository.save(specification);
    }
}
