package com.fourarcade.arcadebackend.mypage;

import com.fourarcade.arcadebackend.quiz.Quiz;
import lombok.Builder;
import lombok.Getter;
import org.springframework.data.domain.Page;

import java.util.List;

@Getter
@Builder
public class MyQuizListResponse {

    private List<MyQuizListItemResponse> content;
    private long totalElements;
    private int totalPages;
    private int currentPage;
    private int size;

    public static MyQuizListResponse from(Page<Quiz> quizPage) {
        return MyQuizListResponse.builder()
                .content(
                        quizPage.getContent().stream()
                                .map(MyQuizListItemResponse::from)
                                .toList()
                )
                .totalElements(quizPage.getTotalElements())
                .totalPages(quizPage.getTotalPages())
                .currentPage(quizPage.getNumber())
                .size(quizPage.getSize())
                .build();
    }
}