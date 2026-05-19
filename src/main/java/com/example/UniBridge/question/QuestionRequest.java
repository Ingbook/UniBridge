package com.example.UniBridge.question;

import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
public class QuestionRequest {

    private Long companyId;
    private String category;
    private String title;
    private String content;
}
