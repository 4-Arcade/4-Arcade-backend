package com.fourarcade.arcadebackend.question;

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
@RequestMapping("/quiz/{quizId}/questions")
@RequiredArgsConstructor
public class QuestionController {

    private final QuestionService questionService;

    @PostMapping
    public ResponseEntity<ApiResponse<QuestionCreateResponse>> createQuestion(
            @AuthenticationPrincipal CustomUserPrincipal principal,
            @PathVariable UUID quizId,
            @Valid @RequestBody QuestionCreateRequest request
    ) {
        QuestionCreateResponse response = questionService.createQuestion(principal.getUserId(), quizId, request);

        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.ok(response));
    }
    @PutMapping("/{questionId}")
    public ResponseEntity<ApiResponse<QuestionCreateResponse>> updateQuestion(
            @AuthenticationPrincipal CustomUserPrincipal principal,
            @PathVariable UUID quizId,
            @PathVariable UUID questionId,
            @Valid @RequestBody QuestionCreateRequest request
    ) {
        QuestionCreateResponse response =
                questionService.updateQuestion(principal.getUserId(), quizId, questionId, request);

        return ResponseEntity.ok(ApiResponse.ok(response));
    }
    @GetMapping("/{questionId}")
    public ResponseEntity<ApiResponse<QuestionDetailResponse>> getQuestionDetail(
            @AuthenticationPrincipal CustomUserPrincipal principal,
            @PathVariable UUID quizId,
            @PathVariable UUID questionId
    ) {
        QuestionDetailResponse response =
                questionService.getQuestionDetail(principal.getUserId(), quizId, questionId);

        return ResponseEntity.ok(ApiResponse.ok(response));
    }
    @DeleteMapping("/{questionId}")
    public ResponseEntity<ApiResponse<Void>> deleteQuestion(
            @AuthenticationPrincipal CustomUserPrincipal principal,
            @PathVariable UUID quizId,
            @PathVariable UUID questionId
    ) {
        questionService.deleteQuestion(principal.getUserId(), quizId, questionId);

        return ResponseEntity.ok(ApiResponse.ok(null));
    }
}