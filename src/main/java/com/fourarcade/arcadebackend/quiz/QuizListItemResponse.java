package com.fourarcade.arcadebackend.quiz;

import lombok.Builder;
import lombok.Getter;

import java.time.OffsetDateTime;
import java.util.UUID;

@Getter
@Builder
public class QuizListItemResponse {

    private UUID id;
    private String title;
    private String description;
    private String category;
    private int questionCount;
    private int playCount;
    private String createdBy;
    private OffsetDateTime createdAt;

    public static QuizListItemResponse from(Quiz quiz) {
        return QuizListItemResponse.builder()
                .id(quiz.getId())
                .title(quiz.getTitle())
                .description(quiz.getDescription())
                .category(quiz.getCategory().getValue())
                .questionCount(quiz.getQuestionCount())
                .playCount(quiz.getPlayCount())
                .createdBy(quiz.getUser().getNickname())
                .createdAt(quiz.getCreatedAt())
                .build();
    }
}