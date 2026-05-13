package com.example.UniBridge.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/students")
@Tag(name = "Student API", description = "학생 관련 API")
public class StudentController {

    @GetMapping("/{studentId}")
    @Operation(summary = "학생 정보 조회", description = "학생 기본 정보를 조회합니다.")
    @ApiResponse(responseCode = "200", description = "학생 조회 성공")
    @ApiResponse(responseCode = "404", description = "학생 정보를 찾을 수 없음")
    public String getStudent(@PathVariable Long studentId) {
        return "학생 정보 조회";
    }

    @PostMapping
    @Operation(summary = "학생 등록", description = "학생 정보를 등록합니다.")
    @ApiResponse(responseCode = "201", description = "학생 등록 성공")
    @ApiResponse(responseCode = "400", description = "잘못된 요청")
    public String createStudent() {
        return "학생 등록 완료";
    }