package com.example.UniBridge.analysis.controller;

import com.example.UniBridge.analysis.dto.AiProfileAnalysisRequest;
import com.example.UniBridge.analysis.dto.AiProfileAnalysisResponse;
import com.example.UniBridge.analysis.dto.AnalysisReportResponse;
import com.example.UniBridge.analysis.dto.GapAnalysisRequest;
import com.example.UniBridge.analysis.dto.GapAnalysisResponse;
import com.example.UniBridge.analysis.dto.GpaCertificationAnalysisRequest;
import com.example.UniBridge.analysis.service.AnalysisService;
import com.example.UniBridge.analysis.service.GapAnalysisService;
import com.example.UniBridge.analysis.service.ProfileAnalysisService;
import com.example.UniBridge.common.response.BaseResponse;
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
@Tag(name = "Analysis API", description = "학점 및 자격증 기반 분석 API")
public class AnalysisController {

    private final AnalysisService analysisService;
    private final GapAnalysisService gapAnalysisService;
    private final ProfileAnalysisService profileAnalysisService;

    @PostMapping("/gpa-certification")
    @Operation(summary = "학점 및 자격증 기반 분석 실행", description = "학점과 보유 자격증 점수로 기업 평균 점수와 비교합니다.")
    @ApiResponse(responseCode = "200", description = "학점 및 자격증 기반 분석 성공")
    @ApiResponse(responseCode = "400", description = "잘못된 요청, 학점 정보 미등록 또는 존재하지 않는 기업")
    @ApiResponse(responseCode = "500", description = "서버 내부 오류")
    public BaseResponse<AnalysisReportResponse> analyzeGpaAndCertification(
            @RequestBody GpaCertificationAnalysisRequest request
    ) {
        return BaseResponse.success("학점 및 자격증 기반 분석 성공", analysisService.analyzeGpaAndCertification(request));
    }

    @PostMapping("/gap")
    @Operation(summary = "AI Gap Analysis 실행", description = "현재 사용자와 선택 동문 스펙을 Ollama LLM으로 1:1 비교합니다.")
    @ApiResponse(responseCode = "200", description = "AI Gap Analysis 성공")
    @ApiResponse(responseCode = "400", description = "잘못된 요청, 스펙 미등록, 존재하지 않는 기업 또는 동문")
    @ApiResponse(responseCode = "500", description = "서버 내부 오류")
    public BaseResponse<GapAnalysisResponse> analyzeGap(@RequestBody GapAnalysisRequest request) {
        return BaseResponse.success("AI Gap 분석이 완료되었습니다.", gapAnalysisService.analyzeGap(request));
    }

    @PostMapping("/profile")
    @Operation(summary = "AI 프로필 분석 실행", description = "사용자가 입력한 프로필 값을 검증한 뒤 AI 분석 결과를 반환합니다.")
    @ApiResponse(responseCode = "200", description = "AI 프로필 분석 성공")
    @ApiResponse(responseCode = "400", description = "잘못된 학점 또는 허용되지 않은 자격증")
    @ApiResponse(responseCode = "500", description = "서버 내부 오류")
    public BaseResponse<AiProfileAnalysisResponse> analyzeProfile(
            @RequestBody AiProfileAnalysisRequest request
    ) {
        return BaseResponse.success("사용자 프로필 분석이 완료되었습니다.", profileAnalysisService.analyzeProfile(request));
    }

    @GetMapping("/me")
    @Operation(summary = "내 분석 결과 목록 조회", description = "userId=1 기준 분석 결과 목록을 createdAt 내림차순으로 조회합니다.")
    @ApiResponse(responseCode = "200", description = "분석 결과 목록 조회 성공")
    @ApiResponse(responseCode = "500", description = "서버 내부 오류")
    public BaseResponse<List<AnalysisReportResponse>> getMyAnalysisReports() {
        return BaseResponse.success("요청 성공", analysisService.getMyAnalysisReports());
    }

    @GetMapping("/{analysisId}")
    @Operation(summary = "분석 결과 상세 조회", description = "특정 분석 결과 상세 정보를 조회합니다.")
    @ApiResponse(responseCode = "200", description = "분석 결과 조회 성공")
    @ApiResponse(responseCode = "404", description = "존재하지 않는 분석 결과")
    @ApiResponse(responseCode = "500", description = "서버 내부 오류")
    public BaseResponse<AnalysisReportResponse> getAnalysisReport(@PathVariable Long analysisId) {
        return BaseResponse.success("요청 성공", analysisService.getAnalysisReport(analysisId));
    }
}
