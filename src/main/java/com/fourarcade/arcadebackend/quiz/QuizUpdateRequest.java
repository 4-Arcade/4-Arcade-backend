package com.fourarcade.arcadebackend.quiz;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
public class QuizUpdateRequest {

    @NotBlank(message = "제목은 필수입니다.")
    @Size(min = 1, max = 50, message = "제목은 1자 이상 50자 이하여야 합니다.")
    private String title;

    @NotBlank(message = "카테고리는 필수입니다.")
    private String category;

    @NotNull(message = "공개 여부는 필수입니다.")
    private Boolean isPublic;
}