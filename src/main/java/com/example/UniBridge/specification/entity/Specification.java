package com.example.UniBridge.specification.entity;

import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "specifications")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Specification {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private Long userId;
    private BigDecimal gpa;
    private BigDecimal maxGpa;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    @Builder
    public Specification(Long userId, BigDecimal gpa, BigDecimal maxGpa) {
        this.userId = userId;
        this.gpa = gpa;
        this.maxGpa = maxGpa;
    }

    public void update(BigDecimal gpa, BigDecimal maxGpa) {
        this.gpa = gpa;
        this.maxGpa = maxGpa;
    }

    @PrePersist
    public void prePersist() {
        LocalDateTime now = LocalDateTime.now();
        this.createdAt = now;
        this.updatedAt = now;
    }

    @PreUpdate
    public void preUpdate() {
        this.updatedAt = LocalDateTime.now();
    }
}
