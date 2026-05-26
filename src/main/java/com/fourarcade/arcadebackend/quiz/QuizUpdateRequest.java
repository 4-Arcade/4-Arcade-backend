package com.fourarcade.arcadebackend.quiz;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
public class QuizUpdateRequest {

    @NotBlank(message = "제목을 입력하지 않았습니다.")
    @Size(min = 1, max = 50, message = "제목은 1자 이상 50자 이하여야 합니다.")
    private String title;

    @Size(max = 200, message = "설명은 200자 이하이어야 합니다.")
    private String description;

    @NotBlank(message = "카테고리를 선택하지 않았습니다.")
    private String category;

    @NotNull(message = "공개 여부를 선택하지 않았습니다.")
    private Boolean isPublic;
}