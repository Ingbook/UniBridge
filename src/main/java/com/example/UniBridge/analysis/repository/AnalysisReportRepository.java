package com.example.UniBridge.analysis.repository;

import com.example.UniBridge.analysis.entity.AnalysisReport;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface AnalysisReportRepository extends JpaRepository<AnalysisReport, Long> {

    List<AnalysisReport> findByUserIdOrderByCreatedAtDesc(Long userId);
}
