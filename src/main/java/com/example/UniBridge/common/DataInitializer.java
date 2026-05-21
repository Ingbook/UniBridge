package com.example.UniBridge.common;

import com.example.UniBridge.company.Company;
import com.example.UniBridge.company.CompanyRepository;
import java.util.List;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.Ordered;

@Configuration
public class DataInitializer {

    @Bean
    public CommandLineRunner initializeCompanyData(CompanyRepository companyRepository) {
        return new CompanyDataRunner(companyRepository);
    }

    private static class CompanyDataRunner implements CommandLineRunner, Ordered {

        private final CompanyRepository companyRepository;

        private CompanyDataRunner(CompanyRepository companyRepository) {
            this.companyRepository = companyRepository;
        }

        @Override
        public int getOrder() {
            return Ordered.HIGHEST_PRECEDENCE;
        }

        @Override
        public void run(String... args) {
            if (companyRepository.count() != 0) {
                return;
            }

            companyRepository.saveAll(List.of(
                    company("TechCorp", "IT", "Backend Developer", 85),
                    company("DataFlow", "AI/Data", "Data Analyst", 88),
                    company("SecureApp", "Security", "Security Engineer", 82),
                    company("CloudNet", "Cloud", "DevOps Engineer", 84),
                    company("PublicIT", "Public Sector", "IT System Manager", 79),
                    company("GreenEnergy", "Energy", "IoT Engineer", 81),
                    company("FinBridge", "FinTech", "Backend Developer", 86),
                    company("HealthSync", "Healthcare IT", "Full Stack Developer", 80),
                    company("EduNext", "EdTech", "Frontend Developer", 78),
                    company("SmartFactory", "Manufacturing IT", "Embedded Software Engineer", 83),

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
        }
    }

    private static Company company(String name, String industry, String mainJobRole, Integer averageScore) {
        return Company.builder()
                .name(name)
                .industry(industry)
                .mainJobRole(mainJobRole)
                .averageScore(averageScore)
                .build();
    }
}
