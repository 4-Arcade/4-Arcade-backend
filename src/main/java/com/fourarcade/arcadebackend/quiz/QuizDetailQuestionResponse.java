package com.fourarcade.arcadebackend.quiz;

import com.fourarcade.arcadebackend.question.Question;
import lombok.Builder;
import lombok.Getter;

import java.util.UUID;

@Getter
@Builder
public class QuizDetailQuestionResponse {
    private UUID id;
    private int orderIndex;
    private String hint;
    private int startSec;
    private int endSec;

    public static QuizDetailQuestionResponse from(Question question){
        return QuizDetailQuestionResponse.builder()
                .id(question.getId())
                .orderIndex(question.getOrderIndex())
                .hint(question.getHint())
                .startSec(question.getStartSec())
                .endSec(question.getEndSec())
                .build();
    }
}
