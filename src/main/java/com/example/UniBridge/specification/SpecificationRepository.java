package com.example.UniBridge.specification;

import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface SpecificationRepository extends JpaRepository<Specification, Long> {

    Optional<Specification> findByUserId(Long userId);

    void deleteByUserId(Long userId);
}
