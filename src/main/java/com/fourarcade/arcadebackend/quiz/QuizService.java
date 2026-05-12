package com.fourarcade.arcadebackend.quiz;

import com.fourarcade.arcadebackend.common.exception.AuthException;
import com.fourarcade.arcadebackend.question.Question;
import com.fourarcade.arcadebackend.question.QuestionRepository;
import org.springframework.scheduling.annotation.Async;
import com.fourarcade.arcadebackend.user.User;
import com.fourarcade.arcadebackend.user.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;

import java.util.UUID;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class QuizService {

    private final QuizRepository quizRepository;
    private final UserRepository userRepository;
    private final QuestionRepository questionRepository;

    @Transactional
    public QuizCreateResponse createQuiz(UUID userId, QuizCreateRequest request) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new AuthException("USER_NOT_FOUND", "유저를 찾을 수 없습니다.", HttpStatus.NOT_FOUND));

        Quiz quiz = Quiz.builder()
                .user(user)
                .title(request.getTitle())
                .description(request.getDescription())
                .category(request.getCategory())
                .isPublic(request.getIsPublic())
                .playCount(0)
                .build();

        Quiz savedQuiz = quizRepository.saveAndFlush(quiz);
        return QuizCreateResponse.from(savedQuiz);
    }

    // RoomService 에서 퀴즈 정보 가져갈 때 사용
    @Transactional(readOnly = true)
    public Quiz getQuizById(UUID quizId) {
        return quizRepository.findById(quizId)
                .orElseThrow(() -> new AuthException("QUIZ_NOT_FOUND", "존재하지 않는 퀴즈입니다.", HttpStatus.NOT_FOUND));
    }
    @Transactional(readOnly = true)
    public QuizDetailResponse getQuizDetail(UUID quizId, UUID userIdOrNull) {
        Quiz quiz = quizRepository.findWithUserById(quizId)
                .orElseThrow(() -> new AuthException("NOT_FOUND", "퀴즈를 찾을 수 없습니다.", HttpStatus.NOT_FOUND));

        boolean isOwner = userIdOrNull != null && quiz.getUser().getId().equals(userIdOrNull);

        if (!quiz.isPublic() && !isOwner) {
            throw new AuthException("FORBIDDEN", "비공개 퀴즈는 제작자만 조회할 수 있습니다.", HttpStatus.FORBIDDEN);
        }

        List<Question> questions = questionRepository.findByQuiz_IdOrderByOrderIndexAsc(quizId);

        List<QuizDetailQuestionResponse> questionResponses = questions.stream()
                .map(QuizDetailQuestionResponse::from)
                .collect(Collectors.toList());

        return QuizDetailResponse.builder()
                .id(quiz.getId())
                .title(quiz.getTitle())
                .description(quiz.getDescription())
                .category(quiz.getCategory().getValue())
                .publicStatus(quiz.isPublic())
                .playCount(quiz.getPlayCount())
                .questionCount(quiz.getQuestionCount())
                .createdBy(quiz.getUser().getNickname())
                .createdAt(quiz.getCreatedAt())
                .updatedAt(quiz.getUpdatedAt())
                .questions(questionResponses)
                .build();
    }

    @Transactional
    public QuizUpdateResponse updateQuiz(UUID userId, UUID quizId, QuizUpdateRequest request) {
        Quiz quiz = quizRepository.findById(quizId)
                .orElseThrow(() -> new AuthException("NOT_FOUND", "퀴즈를 찾을 수 없습니다.", HttpStatus.NOT_FOUND));

        if (!quiz.getUser().getId().equals(userId)) {
            throw new AuthException("FORBIDDEN", "본인 퀴즈만 수정할 수 있습니다.", HttpStatus.FORBIDDEN);
        }

        QuizCategory category = QuizCategory.from(request.getCategory());

        boolean currentIsPublic = quiz.isPublic();
        boolean targetIsPublic = request.getIsPublic();

        if (!currentIsPublic && targetIsPublic) {
            long questionCount = questionRepository.countByQuiz_Id(quizId);
            if (questionCount < 5) {
                throw new AuthException("QUIZ_MIN_QUESTIONS", "공개 퀴즈는 최소 5문제가 필요합니다.", HttpStatus.BAD_REQUEST);
            }
        }

        quiz.update(
                request.getTitle(),
                request.getDescription(),
                category,
                targetIsPublic
        );

        return QuizUpdateResponse.from(quiz);
    }

    @Transactional
    public void deleteQuiz(UUID userId, UUID quizId) {
        Quiz quiz = quizRepository.findById(quizId)
                .orElseThrow(() -> new AuthException("NOT_FOUND", "퀴즈를 찾을 수 없습니다.", HttpStatus.NOT_FOUND));

        if (!quiz.getUser().getId().equals(userId)) {
            throw new AuthException("FORBIDDEN", "본인 퀴즈만 삭제할 수 있습니다.", HttpStatus.FORBIDDEN);
        }

        quizRepository.delete(quiz);
        quizRepository.flush();
    }

    @Transactional(readOnly = true)
    public QuizListResponse getPublicQuizList(int page, int size, String category, String sort) {
        if (page < 0) {
            throw new IllegalArgumentException("페이지 번호는 0 이상이어야 합니다.");
        }

        if (size < 1 || size > 50) {
            throw new IllegalArgumentException("페이지 크기는 1 이상 50 이하이어야 합니다.");
        }

        Sort sortOption = switch (sort) {
            case "latest" -> Sort.by(Sort.Direction.DESC, "createdAt");
            case "popular" -> Sort.by(Sort.Direction.DESC, "playCount")
                    .and(Sort.by(Sort.Direction.DESC, "createdAt"));
            default -> throw new IllegalArgumentException("유효하지 않은 정렬 기준입니다.");
        };

        Pageable pageable = PageRequest.of(page, size, sortOption);

        Page<Quiz> quizPage;

        if (category == null || category.isBlank()) {
            quizPage = quizRepository.findByIsPublicTrue(pageable);
        } else {
            QuizCategory quizCategory = QuizCategory.from(category);
            quizPage = quizRepository.findByIsPublicTrueAndCategory(quizCategory, pageable);
        }

        return QuizListResponse.from(quizPage);
    }

    // 게임 정상 종료 시 플레이 횟수 증가
    @Async
    @Transactional
    public void incrementPlayCount(UUID quizId) {
        Quiz quiz = quizRepository.findById(quizId)
                .orElseThrow(() -> new AuthException("QUIZ_NOT_FOUND", "존재하지 않는 퀴즈입니다.", HttpStatus.NOT_FOUND));

        quiz.incrementPlayCount();
    }

}
