package com.fourarcade.arcadebackend.question;

import com.fourarcade.arcadebackend.common.exception.AuthException;
import com.fourarcade.arcadebackend.quiz.Quiz;
import com.fourarcade.arcadebackend.quiz.QuizRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class QuestionService {

    private final QuestionRepository questionRepository;
    private final QuizRepository quizRepository;

    private static final int MAX_QUESTIONS = 30;

    @Transactional
    public QuestionCreateResponse createQuestion(UUID userId, UUID quizId, QuestionCreateRequest request) {
        Quiz quiz = quizRepository.findById(quizId)
                .orElseThrow(() -> new AuthException("NOT_FOUND", "퀴즈를 찾을 수 없습니다.", HttpStatus.NOT_FOUND));

        if (!quiz.getUser().getId().equals(userId)) {
            throw new AuthException("FORBIDDEN", "본인 퀴즈만 수정할 수 있습니다.", HttpStatus.FORBIDDEN);
        }

        validateTimeRange(request);

        long questionCount = questionRepository.countByQuiz_Id(quizId);
        if (questionCount >= MAX_QUESTIONS) {
            throw new IllegalArgumentException("문제는 최대 30개까지만 추가할 수 있습니다.");
        }

        int nextOrderIndex = questionRepository.findTopByQuiz_IdOrderByOrderIndexDesc(quizId)
                .map(question -> question.getOrderIndex() + 1)
                .orElse(1);

        Question question = Question.builder()
                .quiz(quiz)
                .youtubeUrl(request.getYoutubeUrl())
                .startSec(request.getStartSec())
                .endSec(request.getEndSec())
                .answers(request.getAnswers().toArray(new String[0]))
                .hint(request.getHint())
                .orderIndex(nextOrderIndex)
                .build();

        Question savedQuestion = questionRepository.save(question);
        return QuestionCreateResponse.from(savedQuestion);
    }

    private void validateTimeRange(QuestionCreateRequest request) {
        if (request.getEndSec() <= request.getStartSec()) {
            throw new IllegalArgumentException("종료 시간은 시작 시간보다 커야 합니다.");
        }

        if (request.getEndSec() - request.getStartSec() > 30) {
            throw new IllegalArgumentException("재생 구간은 최대 30초까지 가능합니다.");
        }
    }
    @Transactional
    public QuestionCreateResponse updateQuestion(UUID userId, UUID quizId, UUID questionId, QuestionCreateRequest request) {
        Quiz quiz = quizRepository.findById(quizId)
                .orElseThrow(() -> new AuthException("NOT_FOUND", "퀴즈를 찾을 수 없습니다.", HttpStatus.NOT_FOUND));

        if (!quiz.getUser().getId().equals(userId)) {
            throw new AuthException("FORBIDDEN", "본인 퀴즈만 수정할 수 있습니다.", HttpStatus.FORBIDDEN);
        }

        Question question = questionRepository.findByIdAndQuiz_Id(questionId, quizId)
                .orElseThrow(() -> new AuthException("NOT_FOUND", "문제를 찾을 수 없습니다.", HttpStatus.NOT_FOUND));

        validateTimeRange(request);

        question.update(
                request.getYoutubeUrl(),
                request.getStartSec(),
                request.getEndSec(),
                request.getAnswers().toArray(new String[0]),
                request.getHint()
        );

        return QuestionCreateResponse.from(question);
    }
    @Transactional
    public void deleteQuestion(UUID userId, UUID quizId, UUID questionId) {
        Quiz quiz = quizRepository.findById(quizId)
                .orElseThrow(() -> new AuthException("NOT_FOUND", "퀴즈를 찾을 수 없습니다.", HttpStatus.NOT_FOUND));

        if (!quiz.getUser().getId().equals(userId)) {
            throw new AuthException("FORBIDDEN", "본인 퀴즈만 수정할 수 있습니다.", HttpStatus.FORBIDDEN);
        }

        Question targetQuestion = questionRepository.findByIdAndQuiz_Id(questionId, quizId)
                .orElseThrow(() -> new AuthException("NOT_FOUND", "문제를 찾을 수 없습니다.", HttpStatus.NOT_FOUND));

        int deletedOrderIndex = targetQuestion.getOrderIndex();

        questionRepository.delete(targetQuestion);
        questionRepository.flush();

        List<Question> remainingQuestions = questionRepository.findByQuiz_IdOrderByOrderIndexAsc(quizId);

        for (Question question : remainingQuestions) {
            if (question.getOrderIndex() > deletedOrderIndex) {
                question.updateOrderIndex(question.getOrderIndex() - 1);
            }
        }
    }
}
