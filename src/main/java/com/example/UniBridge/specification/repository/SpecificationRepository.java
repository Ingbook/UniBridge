package com.example.UniBridge.specification.repository;

import com.example.UniBridge.specification.entity.Specification;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface SpecificationRepository extends JpaRepository<Specification, Long> {

    Optional<Specification> findByUserId(Long userId);
}
