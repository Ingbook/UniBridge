package com.example.UniBridge.certification.controller;

import com.example.UniBridge.certification.dto.CertificationResponse;
import com.example.UniBridge.certification.dto.UserCertificationRequest;
import com.example.UniBridge.certification.dto.UserCertificationResponse;
import com.example.UniBridge.certification.service.CertificationService;
import com.example.UniBridge.common.response.BaseResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
@Tag(name = "Certification API", description = "자격증 및 보유 자격증 API")
public class CertificationController {

    private final CertificationService certificationService;

    @GetMapping("/api/certifications")
    @Operation(summary = "자격증 목록 조회", description = "전체 자격증 목록을 category, name 오름차순으로 조회합니다.")
    @ApiResponse(responseCode = "200", description = "자격증 목록 조회 성공")
    public BaseResponse<List<CertificationResponse>> getCertifications() {
        return BaseResponse.success("요청 성공", certificationService.getCertifications());
    }

    @GetMapping("/api/users/me/certifications")
    @Operation(summary = "내 보유 자격증 목록 조회", description = "userId=1 기준 보유 자격증 목록을 조회합니다.")
    @ApiResponse(responseCode = "200", description = "보유 자격증 목록 조회 성공")
    public BaseResponse<List<UserCertificationResponse>> getMyCertifications() {
        return BaseResponse.success("요청 성공", certificationService.getMyCertifications());
    }

    @PostMapping("/api/users/me/certifications")
    @Operation(summary = "내 보유 자격증 등록", description = "userId=1 기준 보유 자격증을 등록합니다.")
    @ApiResponse(responseCode = "200", description = "보유 자격증 등록 성공")
    public BaseResponse<UserCertificationResponse> addMyCertification(@RequestBody UserCertificationRequest request) {
        return BaseResponse.success("보유 자격증 등록 성공", certificationService.addMyCertification(request));
    }

    @DeleteMapping("/api/users/me/certifications/{certificationId}")
    @Operation(summary = "내 보유 자격증 삭제", description = "userId=1 기준 보유 자격증을 삭제합니다.")
    @ApiResponse(responseCode = "200", description = "보유 자격증 삭제 성공")
    public BaseResponse<Void> deleteMyCertification(@PathVariable Long certificationId) {
        certificationService.deleteMyCertification(certificationId);
        return BaseResponse.success("보유 자격증 삭제 성공", null);
    }
}
