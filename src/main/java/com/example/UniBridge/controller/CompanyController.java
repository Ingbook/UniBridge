package com.example.UniBridge.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/companies")
@Tag(name = "Company API", description = "회사 관련 API")
public class CompanyController {

    @GetMapping
    @Operation(summary = "기업 목록 조회", description = "전체 기업 목록을 조회합니다.")
    @ApiResponse(responseCode = "200", description = "기업 목록 조회 성공")
    public String getCompanies() {
        return "기업 목록";
    }

    @GetMapping("/{companyId}")
    @Operation(summary = "기업 상세 조회", description = "기업 상세 정보를 조회합니다.")
    @ApiResponse(responseCode = "200", description = "기업 상세 조회 성공")
    @ApiResponse(responseCode = "404", description = "기업을 찾을 수 없음")
    public String getCompany(@PathVariable Long companyId) {
        return "기업 상세 정보";
    }
}
