package com.fourarcade.arcadebackend.question;

import jakarta.validation.constraints.*;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.List;

@Getter
@NoArgsConstructor
public class QuestionCreateRequest {

    @NotBlank(message = "유튜브 URL은 필수입니다.")
    @Size(max = 255, message = "유튜브 URL은 255자 이하여야 합니다.")
    private String youtubeUrl;

    @NotNull(message = "시작 시간은 필수입니다.")
    @Min(value = 0, message = "시작 시간은 0 이상이어야 합니다.")
    private Integer startSec;

    @NotNull(message = "종료 시간은 필수입니다.")
    @Min(value = 0, message = "종료 시간은 0 이상이어야 합니다.")
    private Integer endSec;

    @NotNull(message = "정답은 필수입니다.")
    @Size(min = 1, max = 5, message = "정답은 1개 이상 5개 이하이어야 합니다.")
    private List<@NotBlank(message = "정답은 비어 있을 수 없습니다.")
    @Size(max = 50, message = "각 정답은 50자 이하여야 합니다.") String> answers;

    @Size(max = 20, message = "힌트는 20자 이하여야 합니다.")
    private String hint;
}