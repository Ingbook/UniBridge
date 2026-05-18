package com.example.UniBridge.analysis;

import com.example.UniBridge.common.BaseResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/analysis")
@RequiredArgsConstructor
@Tag(name = "Analysis API", description = "가중치 기반 Gap Analysis API")
public class AnalysisController {

    private final AnalysisService analysisService;

    @PostMapping("/gap")
    @Operation(summary = "Gap Analysis 실행", description = "등록된 스펙과 기업 평균 합격 점수를 비교해 분석 결과를 생성합니다.")
    @ApiResponse(responseCode = "200", description = "분석 성공")
    @ApiResponse(responseCode = "400", description = "잘못된 요청 또는 스펙 미등록")
    public BaseResponse<AnalysisReportDto> analyzeGap(@RequestBody GapAnalysisRequest request) {
        return BaseResponse.success(analysisService.analyzeGap(request));
    }

    @GetMapping("/me")
    @Operation(summary = "내 분석 결과 목록 조회", description = "userId=1 기준 분석 결과 목록을 조회합니다.")
    @ApiResponse(responseCode = "200", description = "분석 결과 목록 조회 성공")
    public BaseResponse<List<AnalysisReportDto>> getMyReports() {
        return BaseResponse.success(analysisService.getMyReports());
    }

    @GetMapping("/{analysisId}")
    @Operation(summary = "분석 결과 상세 조회", description = "분석 결과 상세 정보를 조회합니다.")
    @ApiResponse(responseCode = "200", description = "분석 결과 조회 성공")
    public BaseResponse<AnalysisReportDto> getReport(@PathVariable Long analysisId) {
        return BaseResponse.success(analysisService.getReport(analysisId));
    }
}
