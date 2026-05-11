package com.fourarcade.arcadebackend.auth.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
public class LogoutRequest {
    @NotBlank(message = "RefreshToken은 필수입니다.")
    private String refreshToken;
}
