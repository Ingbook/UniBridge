package com.example.UniBridge.certification.service;

import com.example.UniBridge.certification.dto.CertificationResponse;
import com.example.UniBridge.certification.dto.UserCertificationRequest;
import com.example.UniBridge.certification.dto.UserCertificationResponse;
import com.example.UniBridge.certification.entity.Certification;
import com.example.UniBridge.certification.entity.UserCertification;
import com.example.UniBridge.certification.repository.CertificationRepository;
import com.example.UniBridge.certification.repository.UserCertificationRepository;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class CertificationService {

    private static final Long CURRENT_USER_ID = 1L;

    private final CertificationRepository certificationRepository;
    private final UserCertificationRepository userCertificationRepository;

    @Transactional(readOnly = true)
    public List<CertificationResponse> getCertifications() {
        return certificationRepository.findAllByOrderByCategoryAscNameAsc().stream()
                .map(CertificationResponse::from)
                .toList();
    }

    @Transactional(readOnly = true)
    public List<UserCertificationResponse> getMyCertifications() {
        return userCertificationRepository.findByUserId(CURRENT_USER_ID).stream()
                .map(UserCertificationResponse::from)
                .toList();
    }

    @Transactional
    public UserCertificationResponse addMyCertification(UserCertificationRequest request) {
        if (request.getCertificationId() == null) {
            throw new IllegalArgumentException("자격증 ID를 입력해 주세요.");
        }
        Certification certification = certificationRepository.findById(request.getCertificationId())
                .orElseThrow(() -> new IllegalArgumentException("존재하지 않는 자격증입니다."));
        if (userCertificationRepository.existsByUserIdAndCertificationId(CURRENT_USER_ID, certification.getId())) {
            throw new IllegalArgumentException("이미 등록된 자격증입니다.");
        }

        UserCertification userCertification = UserCertification.builder()
                .userId(CURRENT_USER_ID)
                .certification(certification)
                .acquiredDate(request.getAcquiredDate())
                .build();

        return UserCertificationResponse.from(userCertificationRepository.save(userCertification));
    }

    @Transactional
    public void deleteMyCertification(Long certificationId) {
        userCertificationRepository.deleteByUserIdAndCertificationId(CURRENT_USER_ID, certificationId);
    }
}
