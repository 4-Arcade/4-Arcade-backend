package com.fourarcade.arcadebackend.auth;

import com.fourarcade.arcadebackend.user.User;
import lombok.Builder;
import lombok.Getter;

import java.util.UUID;

@Getter
@Builder
public class AuthResponse {
    private String accessToken;
    private String refreshToken;
    private UserDto user;

    public static AuthResponse of(String accessToken, String refreshToken, User user) {
        return AuthResponse.builder()
                .accessToken(accessToken)
                .refreshToken(refreshToken)
                .user(new UserDto(user.getId(), user.getNickname(), user.getProfileImg()))
                .build();
    }

    public static AuthResponse withoutRefreshToken(AuthResponse original) {
        return AuthResponse.builder()
                .accessToken(original.getAccessToken())
                .user(original.getUser())
                .build();
    }

    @Getter
    public static class UserDto {
        private final UUID id;
        private final String nickname;
        private final String profileImg;

        public UserDto(UUID id, String nickname, String profileImg) {
            this.id = id;
            this.nickname = nickname;
            this.profileImg = profileImg;
        }
    }
}
