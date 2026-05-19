package com.example.UniBridge.answer;

import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface AnswerRepository extends JpaRepository<Answer, Long> {

    List<Answer> findByQuestionIdOrderByAcceptedDescIdAsc(Long questionId);

    List<Answer> findByQuestionId(Long questionId);

    void deleteByQuestionId(Long questionId);
}
