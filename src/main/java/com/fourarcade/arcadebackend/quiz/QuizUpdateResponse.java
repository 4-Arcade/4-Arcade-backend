package com.fourarcade.arcadebackend.quiz;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Builder;
import lombok.Getter;

import java.time.OffsetDateTime;
import java.util.UUID;

@Getter
@Builder
public class QuizUpdateResponse {

    private UUID id;
    private String title;
    private String description;
    private String category;

    @JsonProperty("isPublic")
    private boolean publicStatus;

    private OffsetDateTime updatedAt;

    public static QuizUpdateResponse from(Quiz quiz) {
        return QuizUpdateResponse.builder()
                .id(quiz.getId())
                .title(quiz.getTitle())
                .description(quiz.getDescription())
                .category(quiz.getCategory().getValue())
                .publicStatus(quiz.isPublic())
                .updatedAt(quiz.getUpdatedAt())
                .build();
    }
}