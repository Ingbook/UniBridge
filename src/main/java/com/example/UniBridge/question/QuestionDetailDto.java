package com.example.UniBridge.question;

import com.example.UniBridge.answer.AnswerDto;
import java.util.List;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class QuestionDetailDto {

    private Long id;
    private Long companyId;
    private String companyName;
    private String category;
    private String title;
    private String content;
    private String writerName;
    private Integer viewCount;
    private List<AnswerDto> answers;

    public static QuestionDetailDto from(Question question, List<AnswerDto> answers) {
        return QuestionDetailDto.builder()
                .id(question.getId())
                .companyId(question.getCompany().getId())
                .companyName(question.getCompany().getName())
                .category(question.getCategory())
                .title(question.getTitle())
                .content(question.getContent())
                .writerName(question.getWriterName())
                .viewCount(question.getViewCount())
                .answers(answers)
                .build();
    }
}
