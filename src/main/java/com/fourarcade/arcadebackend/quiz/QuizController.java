package com.fourarcade.arcadebackend.quiz;

import com.fourarcade.arcadebackend.common.api.ApiResponse;
import com.fourarcade.arcadebackend.common.security.CustomUserPrincipal;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

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
}