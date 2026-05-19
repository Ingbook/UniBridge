package com.example.UniBridge.company;

import com.example.UniBridge.common.BaseResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/companies")
@RequiredArgsConstructor
@Tag(name = "Company API", description = "기업 정보 API")
public class CompanyController {

    private final CompanyService companyService;

    @GetMapping
    @Operation(summary = "기업 목록 조회", description = "키워드, 산업군, 직무 조건으로 기업 목록을 조회합니다.")
    @ApiResponse(responseCode = "200", description = "기업 목록 조회 성공")
    public BaseResponse<List<CompanyDto>> getCompanies(
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) String industry,
            @RequestParam(required = false) String jobRole
    ) {
        return BaseResponse.success(companyService.getCompanies(keyword, industry, jobRole));
    }

    @GetMapping("/{companyId}")
    @Operation(summary = "기업 상세 조회", description = "기업 상세 정보를 조회합니다.")
    @ApiResponse(responseCode = "200", description = "기업 상세 조회 성공")
    @ApiResponse(responseCode = "400", description = "존재하지 않는 기업")
    public BaseResponse<CompanyDto> getCompany(@PathVariable Long companyId) {
        return BaseResponse.success(companyService.getCompany(companyId));
    }

    @GetMapping("/popular")
    @Operation(summary = "인기 기업 조회", description = "평균 합격 점수가 높은 상위 5개 기업을 조회합니다.")
    @ApiResponse(responseCode = "200", description = "인기 기업 조회 성공")
    public BaseResponse<List<CompanyDto>> getPopularCompanies() {
        return BaseResponse.success(companyService.getPopularCompanies());
    }
}
