package com.example.UniBridge.answer;

import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class AnswerDto {

    private Long id;
    private Long questionId;
    private String writerName;
    private String content;
    private Boolean accepted;

    public static AnswerDto from(Answer answer) {
        return AnswerDto.builder()
                .id(answer.getId())
                .questionId(answer.getQuestion().getId())
                .writerName(answer.getWriterName())
                .content(answer.getContent())
                .accepted(answer.getAccepted())
                .build();
    }
}
