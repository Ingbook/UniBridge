package com.example.UniBridge.alumnus;

import com.example.UniBridge.company.CompanyRepository;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class AlumnusService {

    private final CompanyRepository companyRepository;
    private final AlumnusRepository alumnusRepository;

    public List<AlumnusListResponse> getAlumniByCompany(Long companyId) {
        if (companyId == null) {
            throw new IllegalArgumentException("기업 ID를 입력해 주세요.");
        }
        if (!companyRepository.existsById(companyId)) {
            throw new IllegalArgumentException("존재하지 않는 기업입니다.");
        }
        return alumnusRepository.findByCompanyId(companyId).stream()
                .map(AlumnusListResponse::from)
                .toList();
    }
}
