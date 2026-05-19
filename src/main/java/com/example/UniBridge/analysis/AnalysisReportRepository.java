package com.example.UniBridge.analysis;

import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface AnalysisReportRepository extends JpaRepository<AnalysisReport, Long> {

    List<AnalysisReport> findByUserIdOrderByIdDesc(Long userId);
}
