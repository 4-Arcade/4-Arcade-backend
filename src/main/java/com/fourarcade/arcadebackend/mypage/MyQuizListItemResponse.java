package com.fourarcade.arcadebackend.mypage;

import com.fourarcade.arcadebackend.quiz.Quiz;
import lombok.Builder;
import lombok.Getter;

import java.time.OffsetDateTime;
import java.util.UUID;

@Getter
@Builder
public class MyQuizListItemResponse {

    private UUID id;
    private String title;
    private String description;
    private String category;
    private int questionCount;
    private int playCount;
    private OffsetDateTime createdAt;
    private OffsetDateTime updatedAt;

    public static MyQuizListItemResponse from(Quiz quiz) {
        return MyQuizListItemResponse.builder()
                .id(quiz.getId())
                .title(quiz.getTitle())
                .description(quiz.getDescription())
                .category(quiz.getCategory().getValue())
                .questionCount(quiz.getQuestionCount())
                .playCount(quiz.getPlayCount())
                .createdAt(quiz.getCreatedAt())
                .updatedAt(quiz.getUpdatedAt())
                .build();
    }
}