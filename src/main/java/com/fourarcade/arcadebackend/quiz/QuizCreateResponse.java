package com.fourarcade.arcadebackend.quiz;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Builder;
import lombok.Getter;

import java.time.OffsetDateTime;
import java.util.UUID;

@Getter
@Builder
public class QuizCreateResponse {

    private UUID id;
    private String title;
    private String description;
    private String category;

    @JsonProperty("isPublic")
    private boolean publicStatus;

    private int questionCount;
    private OffsetDateTime createdAt;

    public static QuizCreateResponse from(Quiz quiz) {
        return QuizCreateResponse.builder()
                .id(quiz.getId())
                .title(quiz.getTitle())
                .description(quiz.getDescription())
                .category(quiz.getCategory().getValue())
                .publicStatus(quiz.isPublic())
                .questionCount(quiz.getQuestionCount())
                .createdAt(quiz.getCreatedAt())
                .build();
    }
}