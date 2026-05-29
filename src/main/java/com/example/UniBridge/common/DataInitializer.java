package com.example.UniBridge.common;

import com.example.UniBridge.alumnus.Alumnus;
import com.example.UniBridge.alumnus.AlumnusRepository;
import com.example.UniBridge.company.Company;
import com.example.UniBridge.company.CompanyRepository;
import com.example.UniBridge.specification.entity.Specification;
import com.example.UniBridge.specification.repository.SpecificationRepository;
import java.math.BigDecimal;
import java.util.List;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.Ordered;

@Configuration
public class DataInitializer {

    @Bean
    public CommandLineRunner initializeCompanyData(CompanyRepository companyRepository,
                                                   AlumnusRepository alumnusRepository,
                                                   SpecificationRepository specificationRepository) {
        return new CompanyDataRunner(companyRepository, alumnusRepository, specificationRepository);
    }

    private static class CompanyDataRunner implements CommandLineRunner, Ordered {

        private final CompanyRepository companyRepository;
        private final AlumnusRepository alumnusRepository;
        private final SpecificationRepository specificationRepository;

        private CompanyDataRunner(CompanyRepository companyRepository, AlumnusRepository alumnusRepository,
                                  SpecificationRepository specificationRepository) {
            this.companyRepository = companyRepository;
            this.alumnusRepository = alumnusRepository;
            this.specificationRepository = specificationRepository;
        }

        @Override
        public int getOrder() {
            return Ordered.HIGHEST_PRECEDENCE;
        }

        @Override
        public void run(String... args) {
            saveDefaultSpecification();
            if (companyRepository.count() != 0) {
                if (alumnusRepository.count() == 0) {
                    companyRepository.findAll().forEach(this::saveDefaultAlumni);
                }
                return;
            }

            List<Company> companies = companyRepository.saveAll(List.of(
                    company("TechCorp", "IT", "Backend Developer", 85, 3, "Seoul"),
                    company("DataFlow", "AI/Data", "Data Analyst", 88, 3, "Seoul"),
                    company("SecureApp", "Security", "Security Engineer", 82, 3, "Pangyo"),
                    company("CloudNet", "Cloud", "DevOps Engineer", 84, 3, "Seoul"),
                    company("PublicIT", "Public Sector", "IT System Manager", 79, 3, "Daegu"),
                    company("GreenEnergy", "Energy", "IoT Engineer", 81, 3, "Daejeon"),
                    company("FinBridge", "FinTech", "Backend Developer", 86, 3, "Seoul"),
                    company("HealthSync", "Healthcare IT", "Full Stack Developer", 80, 3, "Seoul"),
                    company("EduNext", "EdTech", "Frontend Developer", 78, 3, "Seoul"),
                    company("SmartFactory", "Manufacturing IT", "Embedded Software Engineer", 83, 3, "Gumi"),

                    company("RoboWorks", "Robotics", "Robotics Software Engineer", 87),
                    company("MediaPulse", "Media Platform", "Frontend Developer", 77),
                    company("GameForge", "Game", "Game Server Developer", 84),
                    company("BioNova", "BioTech", "Data Engineer", 82),
                    company("RetailHub", "E-Commerce", "Backend Developer", 80),
                    company("MobilityX", "Mobility", "Mobile App Developer", 83),
                    company("LogisticsOne", "Logistics IT", "Data Engineer", 79),
                    company("AIWorks", "AI", "Machine Learning Engineer", 90),
                    company("DevStudio", "Software", "Full Stack Developer", 76),
                    company("InfraCore", "Infrastructure", "Cloud Engineer", 82),

                    company("CodeWave", "IT Service", "Backend Developer", 78),
                    company("QuantumSoft", "Software R&D", "Research Engineer", 89),
                    company("CyberShield", "Security", "Security Analyst", 85),
                    company("DataMind", "AI/Data", "Data Scientist", 91),
                    company("CloudBridge", "Cloud", "Cloud Engineer", 86),
                    company("AppSquare", "Mobile Service", "Mobile App Developer", 77),
                    company("NetVision", "Network", "Network Engineer", 80),
                    company("ServicePlus", "IT Service", "System Engineer", 75),
                    company("FutureBank", "Finance IT", "Backend Developer", 87),
                    company("SmartGrid", "Energy IT", "Embedded Software Engineer", 82),

                    company("DeepVision", "Computer Vision", "AI Engineer", 92),
                    company("PlatformLabs", "Platform", "Backend Developer", 88),
                    company("OpenCommerce", "E-Commerce", "Frontend Developer", 79),
                    company("PixelCraft", "Design Tech", "UI Developer", 74),
                    company("InsightWorks", "Data Consulting", "Data Analyst", 84),
                    company("AutoSphere", "Automotive IT", "Embedded Software Engineer", 86),
                    company("ConnectWare", "SaaS", "Full Stack Developer", 81),
                    company("UrbanTech", "Smart City", "IoT Engineer", 80),
                    company("NextLogi", "Logistics Platform", "Backend Developer", 78),
                    company("PayLink", "FinTech", "Backend Developer", 85),

                    company("LearnMate", "EdTech", "AI Service Developer", 79),
                    company("MedData", "Healthcare Data", "Data Engineer", 83),
                    company("VisionLab", "AI", "Computer Vision Engineer", 90),
                    company("ServerNest", "Cloud Hosting", "DevOps Engineer", 82),
                    company("ChainWorks", "Blockchain", "Blockchain Developer", 84),
                    company("EcoSoft", "Sustainability IT", "Software Engineer", 76),
                    company("GovTechPlus", "Public Sector", "Software Engineer", 78),
                    company("MarketBridge", "Marketing Tech", "Data Analyst", 77),
                    company("AlphaSecurity", "Security", "Backend Security Developer", 86),
                    company("NovaPlatform", "Platform", "Product Backend Developer", 89)
            ));
            companies.forEach(this::saveDefaultAlumni);
        }

        private void saveDefaultSpecification() {
            specificationRepository.findByUserId(1L)
                    .orElseGet(() -> specificationRepository.save(Specification.builder()
                            .userId(1L)
                            .gpa(new BigDecimal("3.8"))
                            .maxGpa(new BigDecimal("4.5"))
                            .build()));
        }

        private void saveDefaultAlumni(Company company) {
            alumnusRepository.saveAll(List.of(
                    alumnus(company, "James Kim", company.getMainJobRole(), "4.2", 960, 5, 3,
                            "실서비스 배포 경험과 데이터 기반 추천 프로젝트 보유", "상"),
                    alumnus(company, "Sarah Lee", company.getMainJobRole(), "4.0", 920, 4, 2,
                            "협업 기반 웹 서비스 프로젝트와 운영 개선 경험 보유", "중상"),
                    alumnus(company, "Michael Park", company.getMainJobRole(), "4.3", 970, 6, 4,
                            "대규모 트래픽 처리 프로젝트와 클라우드 배포 경험 보유", "상")
            ));
        }
    }

    private static Company company(String name, String industry, String mainJobRole, Integer averageScore) {
        return company(name, industry, mainJobRole, averageScore, 3, "Seoul");
    }

    private static Company company(String name, String industry, String mainJobRole, Integer averageScore,
                                   Integer alumnusCount, String location) {
        return Company.builder()
                .name(name)
                .industry(industry)
                .mainJobRole(mainJobRole)
                .averageScore(averageScore)
                .alumnusCount(alumnusCount)
                .location(location)
                .build();
    }

    private static Alumnus alumnus(Company company, String name, String jobRole, String gpa, Integer languageScore,
                                   Integer certificationCount, Integer awardCount, String projectSummary,
                                   String portfolioLevel) {
        return Alumnus.builder()
                .company(company)
                .name(name)
                .jobRole(jobRole)
                .gpa(new BigDecimal(gpa))
                .maxGpa(BigDecimal.valueOf(4.5))
                .languageType("TOEIC")
                .languageScore(languageScore)
                .certificationCount(certificationCount)
                .awardCount(awardCount)
                .projectSummary(projectSummary)
                .portfolioLevel(portfolioLevel)
                .profileImageUrl("")
                .representativeScore(82)
                .build();
    }
}
