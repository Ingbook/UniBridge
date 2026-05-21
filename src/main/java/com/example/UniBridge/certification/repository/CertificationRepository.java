package com.example.UniBridge.certification.repository;

import com.example.UniBridge.certification.entity.Certification;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface CertificationRepository extends JpaRepository<Certification, Long> {

    Optional<Certification> findByName(String name);

    List<Certification> findAllByOrderByCategoryAscNameAsc();
}
