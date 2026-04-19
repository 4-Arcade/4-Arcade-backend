package com.fourarcade.arcadebackend.room;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.io.Serializable;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class RoomSettings implements Serializable {

    @NotNull
    @Min(value = 5, message = "문제 수는 최소 5개 이상이어야 합니다.")
    @Max(value = 20, message = "문제 수는 최대 20개 이하여야 합니다.")
    private Integer questionCount;

    @NotNull
    @Min(value = 10, message = "제한 시간은 최소 10초 이상이어야 합니다.")
    @Max(value = 30, message = "제한 시간은 최대 30초 이하여야 합니다.")
    private Integer timeLimit;

    @NotNull
    private Boolean showAnswer;

    @Max(value = 1, message = "오답 제한은 null 또는 1이어야 합니다.")
    private Integer wrongAnswerLimit;

}
