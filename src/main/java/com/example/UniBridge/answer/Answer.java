package com.example.UniBridge.answer;

import com.example.UniBridge.question.Question;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "answers")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Answer {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "question_id")
    private Question question;

    private String writerName;

    @Column(length = 4000)
    private String content;
    private Boolean accepted;

    @Builder
    public Answer(Question question, String writerName, String content, Boolean accepted) {
        this.question = question;
        this.writerName = writerName;
        this.content = content;
        this.accepted = accepted;
    }

    public void accept() {
        this.accepted = true;
    }

    public void unaccept() {
        this.accepted = false;
    }
}
