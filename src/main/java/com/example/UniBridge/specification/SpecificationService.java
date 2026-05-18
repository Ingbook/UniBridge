package com.example.UniBridge.specification;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class SpecificationService {

    private static final Long CURRENT_USER_ID = 1L;

    private final SpecificationRepository specificationRepository;

    @Transactional(readOnly = true)
    public SpecificationDto getMySpecification() {
        return SpecificationDto.from(getMySpecificationEntity());
    }

    @Transactional
    public SpecificationDto upsertMySpecification(SpecificationRequest request) {
        validate(request);

        Specification specification = specificationRepository.findByUserId(CURRENT_USER_ID)
                .orElseGet(() -> Specification.builder().userId(CURRENT_USER_ID).build());
        specification.update(
                request.getGpa(),
                request.getMaxGpa(),
                request.getLanguageType(),
                request.getLanguageScore(),
                request.getCertifications(),
                request.getAwards(),
                request.getProjects(),
                request.getInternships(),
                request.getPortfolioUrl()
        );

        return SpecificationDto.from(specificationRepository.save(specification));
    }

    @Transactional
    public void deleteMySpecification() {
        if (!specificationRepository.findByUserId(CURRENT_USER_ID).isPresent()) {
            throw new IllegalArgumentException("등록된 스펙 정보가 없습니다.");
        }
        specificationRepository.deleteByUserId(CURRENT_USER_ID);
    }

    @Transactional(readOnly = true)
    public Specification getMySpecificationForAnalysis() {
        return specificationRepository.findByUserId(CURRENT_USER_ID)
                .orElseThrow(() -> new IllegalArgumentException("AI 분석을 진행하려면 먼저 스펙 정보를 등록해야 합니다."));
    }

    private Specification getMySpecificationEntity() {
        return specificationRepository.findByUserId(CURRENT_USER_ID)
                .orElseThrow(() -> new IllegalArgumentException("등록된 스펙 정보가 없습니다."));
    }

    private void validate(SpecificationRequest request) {
        if (request.getGpa() == null || request.getMaxGpa() == null) {
            throw new IllegalArgumentException("학점 정보를 입력해 주세요.");
        }
        if (request.getMaxGpa().signum() <= 0) {
            throw new IllegalArgumentException("최대 학점은 0보다 커야 합니다.");
        }
    }
}
