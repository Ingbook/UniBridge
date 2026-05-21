package com.example.UniBridge.certification.service;

import com.example.UniBridge.certification.entity.Certification;
import com.example.UniBridge.certification.repository.CertificationRepository;
import java.util.List;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class CertificationDataInitializer {

    @Bean
    public CommandLineRunner initializeCertificationData(CertificationRepository certificationRepository) {
        return args -> {
            if (certificationRepository.count() != 0) {
                return;
            }

            certificationRepository.saveAll(List.of(
                    certification("정보처리기사", "BACKEND", 30, "개발 직무 대표 국가기술자격"),
                    certification("SQLD", "DATABASE", 20, "SQL 및 데이터베이스 기본 역량"),
                    certification("ADsP", "DATA", 20, "데이터 분석 기본 자격"),
                    certification("리눅스마스터 2급", "INFRA", 15, "리눅스 운영체제 기본 역량"),
                    certification("AWS Cloud Practitioner", "CLOUD", 20, "클라우드 기본 역량"),
                    certification("네트워크관리사 2급", "NETWORK", 15, "네트워크 기본 역량"),
                    certification("컴퓨터활용능력 1급", "COMMON", 10, "사무 및 데이터 처리 기본 역량"),
                    certification("OCPJP", "JAVA", 20, "Java 프로그래밍 역량"),
                    certification("빅데이터분석기사", "DATA", 30, "빅데이터 분석 실무 역량"),
                    certification("정보보안기사", "SECURITY", 30, "보안 직무 대표 국가기술자격")
            ));
        };
    }

    private Certification certification(String name, String category, Integer score, String description) {
        return Certification.builder()
                .name(name)
                .category(category)
                .score(score)
                .description(description)
                .build();
    }
}
