package com.fourarcade.arcadebackend.quiz;

import com.fourarcade.arcadebackend.common.exception.AuthException;
import com.fourarcade.arcadebackend.user.User;
import com.fourarcade.arcadebackend.user.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
@RequiredArgsConstructor
public class QuizService {

    private final QuizRepository quizRepository;
    private final UserRepository userRepository;

    @Transactional
    public QuizCreateResponse createQuiz(UUID userId, QuizCreateRequest request) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new AuthException("USER_NOT_FOUND", "유저를 찾을 수 없습니다.", HttpStatus.NOT_FOUND));

        Quiz quiz = Quiz.builder()
                .user(user)
                .title(request.getTitle())
                .category(request.getCategory())
                .isPublic(request.getIsPublic())
                .playCount(0)
                .build();

        Quiz savedQuiz = quizRepository.saveAndFlush(quiz);
        return QuizCreateResponse.from(savedQuiz);
    }
}
