package com.example.UniBridge.question;

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
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/questions")
@RequiredArgsConstructor
@Tag(name = "Question API", description = "질문 게시판 API")
public class QuestionController {

    private final QuestionService questionService;

    @GetMapping
    @Operation(summary = "질문 목록 조회", description = "키워드, 카테고리, 기업 조건으로 질문 목록을 조회합니다.")
    @ApiResponse(responseCode = "200", description = "질문 목록 조회 성공")
    @ApiResponse(responseCode = "500", description = "서버 내부 오류")
    public BaseResponse<List<QuestionDto>> getQuestions(
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) String category,
            @RequestParam(required = false) Long companyId
    ) {
        return BaseResponse.success(questionService.getQuestions(keyword, category, companyId));
    }

    @GetMapping("/{questionId}")
    @Operation(summary = "질문 상세 조회", description = "질문 상세와 답변 목록을 조회하고 조회수를 1 증가시킵니다.")
    @ApiResponse(responseCode = "200", description = "질문 상세 조회 성공")
    @ApiResponse(responseCode = "400", description = "존재하지 않는 질문")
    @ApiResponse(responseCode = "500", description = "서버 내부 오류")
    public BaseResponse<QuestionDetailDto> getQuestion(@PathVariable Long questionId) {
        return BaseResponse.success(questionService.getQuestion(questionId));
    }

    @PostMapping
    @Operation(summary = "질문 등록", description = "익명 학생 이름으로 질문을 등록합니다.")
    @ApiResponse(responseCode = "200", description = "질문 등록 성공")
    @ApiResponse(responseCode = "400", description = "잘못된 요청 또는 존재하지 않는 기업")
    @ApiResponse(responseCode = "500", description = "서버 내부 오류")
    public BaseResponse<QuestionDto> createQuestion(@RequestBody QuestionRequest request) {
        return BaseResponse.success(questionService.createQuestion(request));
    }

    @PutMapping("/{questionId}")
    @Operation(summary = "질문 수정", description = "질문 내용을 수정합니다.")
    @ApiResponse(responseCode = "200", description = "질문 수정 성공")
    @ApiResponse(responseCode = "400", description = "잘못된 요청, 존재하지 않는 질문 또는 기업")
    @ApiResponse(responseCode = "500", description = "서버 내부 오류")
    public BaseResponse<QuestionDto> updateQuestion(@PathVariable Long questionId, @RequestBody QuestionRequest request) {
        return BaseResponse.success(questionService.updateQuestion(questionId, request));
    }

    @DeleteMapping("/{questionId}")
    @Operation(summary = "질문 삭제", description = "질문을 삭제합니다.")
    @ApiResponse(responseCode = "200", description = "질문 삭제 성공")
    @ApiResponse(responseCode = "400", description = "존재하지 않는 질문")
    @ApiResponse(responseCode = "500", description = "서버 내부 오류")
    public BaseResponse<Void> deleteQuestion(@PathVariable Long questionId) {
        questionService.deleteQuestion(questionId);
        return BaseResponse.success(null);
    }

    @PostMapping("/{questionId}/vote")
    @Operation(summary = "질문 추천/취소", description = "질문 추천 상태를 토글합니다.")
    @ApiResponse(responseCode = "200", description = "질문 추천 토글 성공")
    @ApiResponse(responseCode = "400", description = "존재하지 않는 질문")
    @ApiResponse(responseCode = "500", description = "서버 내부 오류")
    public BaseResponse<QuestionDto> voteQuestion(
            @PathVariable Long questionId,
            @RequestParam(defaultValue = "true") boolean isUpvote) {
        return BaseResponse.success(questionService.voteQuestion(questionId, isUpvote));
    }
}
