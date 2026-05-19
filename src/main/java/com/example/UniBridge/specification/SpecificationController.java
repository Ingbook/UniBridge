package com.example.UniBridge.specification;

import com.example.UniBridge.common.BaseResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/specifications")
@RequiredArgsConstructor
@Tag(name = "Specification API", description = "사용자 스펙 API")
public class SpecificationController {

    private final SpecificationService specificationService;

    @GetMapping("/me")
    @Operation(summary = "내 스펙 조회", description = "userId=1 기준 스펙 정보를 조회합니다.")
    @ApiResponse(responseCode = "200", description = "스펙 조회 성공")
    public BaseResponse<SpecificationDto> getMySpecification() {
        return BaseResponse.success(specificationService.getMySpecification());
    }

    @PutMapping("/me")
    @Operation(summary = "내 스펙 등록 또는 수정", description = "userId=1 기준 스펙 정보를 등록하거나 수정합니다.")
    @ApiResponse(responseCode = "200", description = "스펙 저장 성공")
    public BaseResponse<SpecificationDto> upsertMySpecification(@RequestBody SpecificationRequest request) {
        return BaseResponse.success(specificationService.upsertMySpecification(request));
    }

    @DeleteMapping("/me")
    @Operation(summary = "내 스펙 삭제", description = "userId=1 기준 스펙 정보를 삭제합니다.")
    @ApiResponse(responseCode = "200", description = "스펙 삭제 성공")
    public BaseResponse<Void> deleteMySpecification() {
        specificationService.deleteMySpecification();
        return BaseResponse.success(null);
    }
}
