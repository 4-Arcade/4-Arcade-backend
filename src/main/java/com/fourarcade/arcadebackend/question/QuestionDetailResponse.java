package com.fourarcade.arcadebackend.question;

import lombok.Builder;
import lombok.Getter;

import java.util.Arrays;
import java.util.List;

@Getter
@Builder
public class QuestionDetailResponse {

    private String youtubeUrl;
    private int startSec;
    private int endSec;
    private List<String> answers;
    private String hint;

    public static QuestionDetailResponse from(Question question) {
        return QuestionDetailResponse.builder()
                .youtubeUrl(question.getYoutubeUrl())
                .startSec(question.getStartSec())
                .endSec(question.getEndSec())
                .answers(Arrays.asList(question.getAnswers()))
                .hint(question.getHint())
                .build();
    }
}