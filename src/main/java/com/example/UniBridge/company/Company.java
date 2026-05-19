package com.example.UniBridge.company;

import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "companies")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Company {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String name;
    private String industry;
    private String mainJobRole;
    private Integer averageScore;

    @Builder
    public Company(String name, String industry, String mainJobRole, Integer averageScore) {
        this.name = name;
        this.industry = industry;
        this.mainJobRole = mainJobRole;
        this.averageScore = averageScore;
    }
}
