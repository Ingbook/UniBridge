package com.example.UniBridge.common;

import com.example.UniBridge.alumnus.Alumnus;
import com.example.UniBridge.alumnus.AlumnusRepository;
import com.example.UniBridge.company.Company;
import com.example.UniBridge.company.CompanyRepository;
import com.example.UniBridge.specification.repository.SpecificationRepository;
import java.math.BigDecimal;
import java.util.ArrayList;
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
            if (companyRepository.count() != 0) {
                companyRepository.findAll().forEach(this::saveDefaultAlumni);
                return;
            }

            List<Company> companies = companyRepository.saveAll(List.of(
                    company("TechCorp", "IT", "Backend Developer", 85, "Seoul"),
                    company("DataFlow", "AI/Data", "Data Analyst", 88, "Seoul"),
                    company("SecureApp", "Security", "Security Engineer", 82, "Pangyo"),
                    company("CloudNet", "Cloud", "DevOps Engineer", 84, "Seoul"),
                    company("PublicIT", "Public Sector", "IT System Manager", 79, "Daegu"),
                    company("GreenEnergy", "Energy", "IoT Engineer", 81, "Daejeon"),
                    company("FinBridge", "FinTech", "Backend Developer", 86, "Seoul"),
                    company("HealthSync", "Healthcare IT", "Full Stack Developer", 80, "Seoul"),
                    company("EduNext", "EdTech", "Frontend Developer", 78, "Seoul"),
                    company("SmartFactory", "Manufacturing IT", "Embedded Software Engineer", 83, "Gumi"),

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

        private void saveDefaultAlumni(Company company) {
            long existingCount = alumnusRepository.countByCompanyId(company.getId());
            int targetCount = desiredAlumnusCount(company.getName());
            if (existingCount >= targetCount) {
                return;
            }

            List<Alumnus> alumni = new ArrayList<>();
            for (int index = (int) existingCount; index < targetCount; index++) {
                alumni.add(alumnus(company, index));
            }
            alumnusRepository.saveAll(alumni);
        }
    }

    private static Company company(String name, String industry, String mainJobRole, Integer averageScore) {
        return company(name, industry, mainJobRole, averageScore, "Seoul");
    }

    private static Company company(String name, String industry, String mainJobRole, Integer averageScore,
                                   String location) {
        return Company.builder()
                .name(name)
                .industry(industry)
                .mainJobRole(mainJobRole)
                .averageScore(averageScore)
                .alumnusCount(desiredAlumnusCount(name))
                .location(location)
                .build();
    }

    private static Alumnus alumnus(Company company, int index) {
        String name = alumnusName(company.getName(), index);
        String jobRole = jobRole(company, index);
        int score = 70 + Math.floorMod(company.getName().hashCode() + index * 7, 26);
        int languageScore = 820 + Math.floorMod(company.getName().hashCode() + index * 17, 16) * 10;
        int certificationCount = 2 + Math.floorMod(company.getName().hashCode() + index, 5);
        int awardCount = Math.floorMod(company.getName().hashCode() + index * 3, 5);
        String gpa = "%.1f".formatted(3.5 + Math.floorMod(company.getName().hashCode() + index, 9) / 10.0);
        String certificationSummary = certificationSummary(index, certificationCount);
        String projectSummary = "%s 분야 %s 프로젝트 경험 보유".formatted(company.getIndustry(), jobRole);
        String portfolioDescription = "%s 직무 중심 포트폴리오".formatted(jobRole);
        String portfolioLevel = index % 3 == 0 ? "상" : index % 3 == 1 ? "중상" : "중";
        return alumnus(company, name, jobRole, gpa, languageScore, certificationCount, awardCount,
                certificationSummary, projectSummary, portfolioDescription, portfolioLevel, score);
    }

    private static Alumnus alumnus(Company company, String name, String jobRole, String gpa, Integer languageScore,
                                   Integer certificationCount, Integer awardCount, String certificationSummary,
                                   String projectSummary, String portfolioDescription, String portfolioLevel,
                                   Integer representativeScore) {
        return Alumnus.builder()
                .company(company)
                .name(name)
                .jobRole(jobRole)
                .gpa(new BigDecimal(gpa))
                .maxGpa(BigDecimal.valueOf(4.5))
                .languageType("TOEIC")
                .languageScore(languageScore)
                .certificationCount(certificationCount)
                .certificationSummary(certificationSummary)
                .awardCount(awardCount)
                .projectSummary(projectSummary)
                .portfolioDescription(portfolioDescription)
                .portfolioLevel(portfolioLevel)
                .profileImageUrl("")
                .representativeScore(representativeScore)
                .build();
    }

    private static int desiredAlumnusCount(String companyName) {
        return switch (companyName) {
            case "DeepVision" -> 7;
            case "DataMind" -> 9;
            case "AIWorks" -> 5;
            case "VisionLab" -> 8;
            case "QuantumSoft" -> 6;
            case "NovaPlatform" -> 11;
            default -> 3 + Math.floorMod(companyName.hashCode(), 10);
        };
    }

    private static String alumnusName(String companyName, int index) {
        String[] firstNames = {"James", "Sarah", "Michael", "Emily", "Daniel", "Hannah", "David", "Olivia",
                "Kevin", "Sophia", "Jason", "Grace"};
        String[] lastNames = {"Kim", "Lee", "Park", "Choi", "Jung", "Kang", "Yoon", "Lim", "Han", "Seo"};
        int offset = Math.floorMod(companyName.hashCode(), firstNames.length);
        return firstNames[(offset + index) % firstNames.length] + " "
                + lastNames[Math.floorMod(offset + index * 2, lastNames.length)];
    }

    private static String jobRole(Company company, int index) {
        String industry = company.getIndustry() == null ? "" : company.getIndustry().toLowerCase();
        String[] roles;
        if (industry.contains("vision")) {
            roles = new String[]{"Computer Vision Engineer", "AI Engineer", "Machine Learning Engineer"};
        } else if (industry.contains("ai") || industry.contains("data")) {
            roles = new String[]{"AI Engineer", "Data Scientist", "Machine Learning Engineer", "Data Engineer"};
        } else if (industry.contains("platform")) {
            roles = new String[]{"Platform Engineer", "Backend Developer", "Product Backend Developer"};
        } else if (industry.contains("cloud")) {
            roles = new String[]{"Cloud Engineer", "DevOps Engineer", "Platform Engineer"};
        } else {
            roles = new String[]{company.getMainJobRole(), "Backend Developer", "Platform Engineer", "Data Scientist"};
        }
        return roles[index % roles.length];
    }

    private static String certificationSummary(int index, int certificationCount) {
        String[] certifications = {"정보처리기사", "SQLD", "ADsP", "AWS Cloud Practitioner", "리눅스마스터 2급", "컴퓨터활용능력 1급"};
        List<String> selected = new ArrayList<>();
        for (int i = 0; i < certificationCount; i++) {
            selected.add(certifications[(index + i) % certifications.length]);
        }
        return String.join(", ", selected);
    }
}
