package com.example.UniBridge.company;

import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class CompanyService {

    private final CompanyRepository companyRepository;

    public List<CompanyDto> getCompanies(String keyword, String industry, String jobRole) {
        return companyRepository.findAll().stream()
                .filter(company -> !StringUtils.hasText(keyword)
                        || company.getName().contains(keyword)
                        || company.getIndustry().contains(keyword)
                        || company.getMainJobRole().contains(keyword))
                .filter(company -> !StringUtils.hasText(industry) || company.getIndustry().equalsIgnoreCase(industry))
                .filter(company -> !StringUtils.hasText(jobRole) || company.getMainJobRole().equalsIgnoreCase(jobRole))
                .map(CompanyDto::from)
                .toList();
    }

    public CompanyDto getCompany(Long companyId) {
        return CompanyDto.from(getCompanyEntity(companyId));
    }

    public List<CompanyDto> getPopularCompanies() {
        return companyRepository.findTop5ByOrderByAverageScoreDesc().stream()
                .map(CompanyDto::from)
                .toList();
    }

    public Company getCompanyEntity(Long companyId) {
        return companyRepository.findById(companyId)
                .orElseThrow(() -> new IllegalArgumentException("존재하지 않는 기업입니다."));
    }
}
