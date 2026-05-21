package com.example.UniBridge.certification.entity;

import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import java.time.LocalDate;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "user_certifications")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class UserCertification {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private Long userId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "certification_id")
    private Certification certification;

    private LocalDate acquiredDate;

    @Builder
    public UserCertification(Long userId, Certification certification, LocalDate acquiredDate) {
        this.userId = userId;
        this.certification = certification;
        this.acquiredDate = acquiredDate;
    }
}
