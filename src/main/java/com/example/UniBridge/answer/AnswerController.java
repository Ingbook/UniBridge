package com.example.UniBridge.answer;

import com.example.UniBridge.common.response.BaseResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
@Tag(name = "Answer API", description = "답변 API")
public class AnswerController {

    private final AnswerService answerService;

    @PostMapping("/api/questions/{questionId}/answers")
    @Operation(summary = "답변 등록", description = "익명 현직자 이름으로 질문에 답변을 등록합니다.")
    @ApiResponse(responseCode = "200", description = "답변 등록 성공")
    @ApiResponse(responseCode = "400", description = "존재하지 않는 질문 또는 잘못된 요청")
    @ApiResponse(responseCode = "500", description = "서버 내부 오류")
    public BaseResponse<AnswerDto> createAnswer(@PathVariable Long questionId, @RequestBody AnswerRequest request) {
        return BaseResponse.success(answerService.createAnswer(questionId, request));
    }

    @PatchMapping("/api/answers/{answerId}/accept")
    @Operation(summary = "답변 채택", description = "선택한 답변을 채택하고 같은 질문의 다른 답변 채택 상태를 해제합니다.")
    @ApiResponse(responseCode = "200", description = "답변 채택 성공")
    @ApiResponse(responseCode = "400", description = "존재하지 않는 답변")
    @ApiResponse(responseCode = "500", description = "서버 내부 오류")
    public BaseResponse<AnswerDto> acceptAnswer(@PathVariable Long answerId) {
        return BaseResponse.success(answerService.acceptAnswer(answerId));
    }
}
