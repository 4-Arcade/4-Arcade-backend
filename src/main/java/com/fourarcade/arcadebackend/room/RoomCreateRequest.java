package com.fourarcade.arcadebackend.room;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.UUID;

@Getter
@NoArgsConstructor
public class RoomCreateRequest {

    @NotNull(message = "quizId는 필수입니다.")
    private UUID quizId;

    @NotBlank(message = "닉네임은 필수입니다.")
    @Size(min = 1, max = 16, message = "닉네임은 1자 이상 16자 이하여야 합니다.")
    private String nickname;

    @NotNull(message = "설정값은 필수입니다.")
    @Valid
    private RoomSettings settings;

}
