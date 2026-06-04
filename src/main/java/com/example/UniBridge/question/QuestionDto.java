package com.example.UniBridge.question;

import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class QuestionDto {

    private Long id;
    private Long companyId;
    private String companyName;
    private String category;
    private String title;
    private String content;
    private String writerName;
    private Integer viewCount;
    private Integer votes;

    public static QuestionDto from(Question question) {
        return QuestionDto.builder()
                .id(question.getId())
                .companyId(question.getCompany() != null ? question.getCompany().getId() : null)
                .companyName(question.getCompany() != null ? question.getCompany().getName() : "Random")
                .category(question.getCategory())
                .title(question.getTitle())
                .content(question.getContent())
                .writerName(question.getWriterName())
                .viewCount(question.getViewCount())
                .votes(question.getVotes())
                .build();
    }
}
