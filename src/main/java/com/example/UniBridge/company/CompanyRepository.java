package com.example.UniBridge.company;

import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface CompanyRepository extends JpaRepository<Company, Long> {

    List<Company> findTop5ByOrderByAverageScoreDesc();

    List<Company> findTop8ByOrderByAverageScoreDesc();

}
