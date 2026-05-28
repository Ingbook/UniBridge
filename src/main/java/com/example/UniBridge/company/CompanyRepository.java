package com.example.UniBridge.company;

import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface CompanyRepository extends JpaRepository<Company, Long> {

    List<Company> findTop5ByOrderByAverageScoreDesc();

    List<Company> findTop6ByOrderByAverageScoreDesc();

    @Query(value = "SELECT COUNT(*) FROM alumni WHERE company_id = :companyId", nativeQuery = true)
    Integer countAlumniByCompanyId(@Param("companyId") Long companyId);
}
