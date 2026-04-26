package com.fourarcade.arcadebackend.question;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface QuestionRepository extends JpaRepository<Question, UUID> {

    long countByQuiz_Id(UUID quizId);

    List<Question> findByQuiz_IdOrderByOrderIndexAsc(UUID quizId);

    Optional<Question> findTopByQuiz_IdOrderByOrderIndexDesc(UUID quizId);

    Optional<Question> findByIdAndQuiz_Id(UUID id, UUID quizId);
}