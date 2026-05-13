package com.example.UniBridge.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/analysis")
@Tag(name = "Analysis API", description = "AI 역량 분석 API")
public class AnalysisController {

    @PostMapping
    @Operation(summary = "AI 역량 분석", description = "사용자의 스펙을 분석합니다.")
    @ApiResponse(responseCode = "200", description = "AI 분석 성공")
    @ApiResponse(responseCode = "400", description = "잘못된 요청 데이터")
    public String analyzeSpec() {
        return "AI 분석 결과";
    }

    @GetMapping("/{analysisId}")
    @Operation(summary = "분석 결과 조회", description = "AI 분석 결과를 조회합니다.")
    @ApiResponse(responseCode = "200", description = "분석 결과 조회 성공")
    @ApiResponse(responseCode = "404", description = "분석 결과를 찾을 수 없음")
    public String getAnalysis(@PathVariable Long analysisId) {
        return "분석 결과 조회";
    }
}