package com.example.UniBridge.specification.service;

import com.example.UniBridge.certification.entity.Certification;
import com.example.UniBridge.certification.entity.UserCertification;
import com.example.UniBridge.certification.repository.CertificationRepository;
import com.example.UniBridge.certification.repository.UserCertificationRepository;
import com.example.UniBridge.specification.dto.ProfileEditDto;
import com.example.UniBridge.specification.dto.SpecificationResponse;
import com.example.UniBridge.specification.entity.Specification;
import com.example.UniBridge.specification.repository.SpecificationRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class SpecificationService {

    private static final Long CURRENT_USER_ID = 1L;

    private final SpecificationRepository specificationRepository;
    private final UserCertificationRepository userCertificationRepository;
    private final CertificationRepository certificationRepository;

    @Transactional(readOnly = true)
    public SpecificationResponse getMySpecification() {
        Specification spec = specificationRepository.findByUserId(CURRENT_USER_ID)
                .orElse(new Specification(CURRENT_USER_ID));

        List<String> certificationNames = userCertificationRepository.findByUserId(CURRENT_USER_ID)
                .stream()
                .map(userCert -> userCert.getCertification().getName())
                .collect(Collectors.toList());

        return SpecificationResponse.from(spec, certificationNames);
    }

    @Transactional(readOnly = true)
    public ProfileEditDto getProfileForEdit() {
        Specification spec = specificationRepository.findByUserId(CURRENT_USER_ID)
                .orElse(new Specification(CURRENT_USER_ID)); // Create a new one if it doesn't exist

        List<Long> certificationIds = userCertificationRepository.findByUserId(CURRENT_USER_ID)
                .stream()
                .map(userCert -> userCert.getCertification().getId())
                .collect(Collectors.toList());

        return ProfileEditDto.from(spec, certificationIds);
    }

    @Transactional
    public void saveOrUpdateProfile(ProfileEditDto dto) {
        Specification spec = specificationRepository.findByUserId(CURRENT_USER_ID)
                .orElseGet(() -> {
                    Specification newSpec = Specification.builder().userId(CURRENT_USER_ID).build();
                    return specificationRepository.save(newSpec);
                });

        spec.update(
                dto.getGpa(),
                dto.getMaxGpa(),
                "TOEIC", // Hardcoded as per previous request
                dto.getLanguageScore(),
                dto.getAwardCount(),
                dto.getProjectSummary(),
                dto.getPortfolioDescription()
        );
        specificationRepository.save(spec);

        // Handle certifications
        userCertificationRepository.deleteByUserId(CURRENT_USER_ID);
        if (dto.getCertificationIds() != null && !dto.getCertificationIds().isEmpty()) {
            List<UserCertification> userCerts = dto.getCertificationIds().stream()
                    .map(certId -> {
                        Certification certification = certificationRepository.findById(certId)
                                .orElseThrow(() -> new IllegalArgumentException("Invalid certification ID: " + certId));
                        return UserCertification.builder()
                                .userId(CURRENT_USER_ID)
                                .certification(certification)
                                .build();
                    })
                    .collect(Collectors.toList());
            userCertificationRepository.saveAll(userCerts);
        }
    }
}
