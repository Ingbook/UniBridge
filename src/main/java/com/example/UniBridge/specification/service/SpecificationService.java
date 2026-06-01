package com.example.UniBridge.specification.service;

import com.example.UniBridge.specification.dto.SpecificationRequest;
import com.example.UniBridge.specification.dto.SpecificationResponse;
import com.example.UniBridge.specification.entity.Specification;
import com.example.UniBridge.specification.repository.SpecificationRepository;
import java.math.BigDecimal;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class SpecificationService {

    private static final Long CURRENT_USER_ID = 1L;

    private final SpecificationRepository specificationRepository;

    @Transactional(readOnly = true)
    public SpecificationResponse getMySpecification() {
        return SpecificationResponse.from(getMySpecificationEntity());
    }

    @Transactional
    public SpecificationResponse saveMySpecification(SpecificationRequest request) {
        validateGpa(request.getGpa(), request.getMaxGpa());

        Specification specification = specificationRepository.findByUserId(CURRENT_USER_ID)
                .orElseGet(() -> Specification.builder().userId(CURRENT_USER_ID).build());
        specification.update(request.getGpa(), request.getMaxGpa(),
                keepExistingWhenNull(request.getLanguageType(), specification.getLanguageType()),
                keepExistingWhenNull(request.getLanguageScore(), specification.getLanguageScore()),
                keepExistingWhenNull(request.getAwardCount(), specification.getAwardCount()),
                keepExistingWhenNull(request.getProjectSummary(), specification.getProjectSummary()),
                keepExistingWhenNull(request.getPortfolioDescription(), specification.getPortfolioDescription()));

        return SpecificationResponse.from(specificationRepository.save(specification));
    }

    public void validateGpa(BigDecimal gpa, BigDecimal maxGpa) {
        if (gpa == null) {
            throw new IllegalArgumentException("학점을 입력해 주세요.");
        }
        if (maxGpa == null || maxGpa.signum() <= 0) {
            throw new IllegalArgumentException("최대 학점은 0보다 커야 합니다.");
        }
        if (gpa.signum() < 0) {
            throw new IllegalArgumentException("학점은 0 이상이어야 합니다.");
        }
        if (gpa.compareTo(maxGpa) > 0) {
            throw new IllegalArgumentException("학점은 최대 학점보다 클 수 없습니다.");
        }
    }

    @Transactional(readOnly = true)
    public Specification getMySpecificationEntityForAnalysis() {
        return specificationRepository.findByUserId(CURRENT_USER_ID)
                .orElseThrow(() -> new IllegalArgumentException("분석을 진행하려면 먼저 학점 정보를 등록해야 합니다."));
    }

    private Specification getMySpecificationEntity() {
        return specificationRepository.findByUserId(CURRENT_USER_ID)
                .orElseThrow(() -> new IllegalArgumentException("등록된 스펙 정보가 없습니다."));
    }

    private String keepExistingWhenNull(String requestedValue, String existingValue) {
        return requestedValue == null ? existingValue : requestedValue;
    }

    private Integer keepExistingWhenNull(Integer requestedValue, Integer existingValue) {
        return requestedValue == null ? existingValue : requestedValue;
    }
}
