package com.fourarcade.arcadebackend.question;

import lombok.Builder;
import lombok.Getter;

import java.time.OffsetDateTime;
import java.util.UUID;

@Getter
@Builder
public class QuestionCreateResponse {

    private UUID id;
    private UUID quizId;
    private int orderIndex;
    private int startSec;
    private int endSec;
    private String hint;
    private OffsetDateTime createdAt;

    public static QuestionCreateResponse from(Question question) {
        return QuestionCreateResponse.builder()
                .id(question.getId())
                .quizId(question.getQuiz().getId())
                .orderIndex(question.getOrderIndex())
                .startSec(question.getStartSec())
                .endSec(question.getEndSec())
                .hint(question.getHint())
                .createdAt(question.getCreatedAt())
                .build();
    }
}