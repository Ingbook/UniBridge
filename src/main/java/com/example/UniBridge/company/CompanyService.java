package com.example.UniBridge.company;

import com.example.UniBridge.alumnus.AlumnusRepository;
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
    private final AlumnusRepository alumnusRepository;

    public List<CompanyDto> getCompanies(String keyword, String industry, String jobRole) {
        return companyRepository.findAll().stream()
                .filter(company -> !StringUtils.hasText(keyword)
                        || company.getName().contains(keyword)
                        || company.getIndustry().contains(keyword)
                        || company.getMainJobRole().contains(keyword))
                .filter(company -> !StringUtils.hasText(industry) || company.getIndustry().equalsIgnoreCase(industry))
                .filter(company -> !StringUtils.hasText(jobRole) || company.getMainJobRole().equalsIgnoreCase(jobRole))
                .map(company -> CompanyDto.from(company, countAlumni(company)))
                .toList();
    }

    public CompanyDto getCompany(Long companyId) {
        Company company = getCompanyEntity(companyId);
        return CompanyDto.from(company, countAlumni(company));
    }

    public List<CompanyDto> getPopularCompanies() {
        return companyRepository.findTop6ByOrderByAverageScoreDesc().stream()
                .limit(5)
                .map(company -> CompanyDto.from(company, countAlumni(company)))
                .toList();
    }

    public List<CompanyHomeDto> getFeaturedCompanies() {
        List<Company> companies = companyRepository.findTop6ByOrderByAverageScoreDesc();
        return companies.stream()
                .map(company -> CompanyHomeDto.from(company, countAlumni(company)))
                .toList();
    }

    public List<CompanyHomeDto> getRecommendedCompanies() {
        List<Company> companies = companyRepository.findAll();
        return companies.stream()
                .map(company -> CompanyHomeDto.from(company, countAlumni(company)))
                .toList();
    }

    public CompanyHomeDto getCompanyById(Long companyId) {
        Company company = getCompanyEntity(companyId);
        return CompanyHomeDto.from(company, countAlumni(company));
    }

    public Company getCompanyEntity(Long companyId) {
        return companyRepository.findById(companyId)
                .orElseThrow(() -> new IllegalArgumentException("존재하지 않는 기업입니다."));
    }

    private int countAlumni(Company company) {
        return Math.toIntExact(alumnusRepository.countByCompanyId(company.getId()));
    }
}
