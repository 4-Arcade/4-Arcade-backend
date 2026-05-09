package com.fourarcade.arcadebackend.quiz;

import com.fourarcade.arcadebackend.common.api.ApiResponse;
import com.fourarcade.arcadebackend.common.security.CustomUserPrincipal;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/quiz")
@RequiredArgsConstructor
public class QuizController {

    private final QuizService quizService;

    @PostMapping
    public ResponseEntity<ApiResponse<QuizCreateResponse>> createQuiz(
            @AuthenticationPrincipal CustomUserPrincipal principal,
            @Valid @RequestBody QuizCreateRequest request
    ) {
        QuizCreateResponse response = quizService.createQuiz(principal.getUserId(), request);

        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.ok(response));
    }
    @GetMapping
    public ResponseEntity<ApiResponse<QuizListResponse>> getPublicQuizList(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "12") int size,
            @RequestParam(required = false) String category,
            @RequestParam(defaultValue = "latest") String sort
    ) {
        QuizListResponse response = quizService.getPublicQuizList(page, size, category, sort);

        return ResponseEntity.ok(ApiResponse.ok(response));
    }
    @GetMapping("/{quizId}")
    public ResponseEntity<ApiResponse<QuizDetailResponse>> getQuizDetail(
            @PathVariable UUID quizId,
            @AuthenticationPrincipal CustomUserPrincipal principal
    ) {
        UUID userId = principal != null ? principal.getUserId() : null;

        QuizDetailResponse response = quizService.getQuizDetail(quizId, userId);

        return ResponseEntity.ok(ApiResponse.ok(response));
    }
    @PutMapping("/{quizId}")
    public ResponseEntity<ApiResponse<QuizUpdateResponse>> updateQuiz(
            @AuthenticationPrincipal CustomUserPrincipal principal,
            @PathVariable UUID quizId,
            @Valid @RequestBody QuizUpdateRequest request
    ) {
        QuizUpdateResponse response = quizService.updateQuiz(principal.getUserId(), quizId, request);

        return ResponseEntity.ok(ApiResponse.ok(response));
    }
    @DeleteMapping("/{quizId}")
    public ResponseEntity<ApiResponse<Void>> deleteQuiz(
            @AuthenticationPrincipal CustomUserPrincipal principal,
            @PathVariable UUID quizId
    ){
        quizService.deleteQuiz(principal.getUserId(),quizId);

        return ResponseEntity.ok(ApiResponse.ok(null));
    }
}