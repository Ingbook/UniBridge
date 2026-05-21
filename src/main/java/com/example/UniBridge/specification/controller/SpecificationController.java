package com.example.UniBridge.specification.controller;

import com.example.UniBridge.common.response.BaseResponse;
import com.example.UniBridge.specification.dto.SpecificationRequest;
import com.example.UniBridge.specification.dto.SpecificationResponse;
import com.example.UniBridge.specification.service.SpecificationService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/specifications")
@RequiredArgsConstructor
@Tag(name = "Specification API", description = "사용자 학점 정보 API")
public class SpecificationController {

    private final SpecificationService specificationService;

    @GetMapping("/me")
    @Operation(summary = "내 학점 정보 조회", description = "userId=1 기준 학점 정보를 조회합니다.")
    @ApiResponse(responseCode = "200", description = "학점 정보 조회 성공")
    public BaseResponse<SpecificationResponse> getMySpecification() {
        return BaseResponse.success("요청 성공", specificationService.getMySpecification());
    }

    @PutMapping("/me")
    @Operation(summary = "내 학점 정보 저장", description = "userId=1 기준 학점 정보를 등록하거나 수정합니다.")
    @ApiResponse(responseCode = "200", description = "스펙 저장 성공")
    public BaseResponse<SpecificationResponse> saveMySpecification(@RequestBody SpecificationRequest request) {
        return BaseResponse.success("스펙 저장 성공", specificationService.saveMySpecification(request));
    }
}
