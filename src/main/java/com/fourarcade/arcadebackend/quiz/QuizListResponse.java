package com.fourarcade.arcadebackend.quiz;

import lombok.Builder;
import lombok.Getter;
import org.springframework.data.domain.Page;

import java.util.List;

@Getter
@Builder
public class QuizListResponse {

    private List<QuizListItemResponse> content;
    private long totalElements;
    private int totalPages;
    private int currentPage;
    private int size;

    public static QuizListResponse from(Page<Quiz> quizPage) {
        return QuizListResponse.builder()
                .content(
                        quizPage.getContent().stream()
                                .map(QuizListItemResponse::from)
                                .toList()
                )
                .totalElements(quizPage.getTotalElements())
                .totalPages(quizPage.getTotalPages())
                .currentPage(quizPage.getNumber())
                .size(quizPage.getSize())
                .build();
    }
}