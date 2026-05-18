// Alumni.java
package entity; // 패키지명은 실제 프로젝트에 맞게 수정하세요

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "alumni")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
@Builder
public class Alumni {

    // 고유 식별자 (PK)
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // 졸업 학점
    @Column(name = "gpa")
    private Double gpa;

    // 어학 점수 (예: 토익 점수)
    @Column(name = "language_score")
    private Integer languageScore;

    // 자격증 개수
    @Column(name = "certification_count")
    private Integer certificationCount;

    // 포트폴리오 수준 (예: "Basic", "Comprehensive")
    @Column(name = "portfolio_level")
    private String portfolioLevel;

    // 합격한 회사 (N:1 연관관계)
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "company_id")
    private Company company;

    // 졸업한 학과 (N:1 연관관계)
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "department_id")
    private Department department;
}