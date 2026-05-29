package com.example.UniBridge.alumnus;

import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface AlumnusRepository extends JpaRepository<Alumnus, Long> {

    List<Alumnus> findByCompanyId(Long companyId);

    Optional<Alumnus> findByCompanyIdAndId(Long companyId, Long alumnusId);
}
