package com.fourarcade.arcadebackend.quiz;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Builder;
import lombok.Getter;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

@Getter
@Builder
public class QuizDetailResponse {

    private UUID id;
    private String title;
    private String category;
    @JsonProperty("isPublic")
    private boolean publicStatus;
    private int playCount;
    private int questionCount;
    private String createdBy;
    private OffsetDateTime createdAt;
    private OffsetDateTime updatedAt;
    private List<QuizDetailQuestionResponse> questions;
}