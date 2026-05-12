package com.fourarcade.arcadebackend.mypage;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
public class NicknameUpdateRequest {

    @NotBlank(message = "닉네임은 필수입니다.")
    @Size(min = 1, max = 16, message = "닉네임은 1자 이상 16자 이하이어야 합니다.")
    private String nickname;
}
