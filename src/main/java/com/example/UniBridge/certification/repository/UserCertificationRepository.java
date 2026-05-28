package com.example.UniBridge.certification.repository;

import com.example.UniBridge.certification.entity.UserCertification;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.transaction.annotation.Transactional;

public interface UserCertificationRepository extends JpaRepository<UserCertification, Long> {

    List<UserCertification> findByUserId(Long userId);

    boolean existsByUserIdAndCertificationId(Long userId, Long certificationId);

    @Transactional
    void deleteByUserIdAndCertificationId(Long userId, Long certificationId);
}
