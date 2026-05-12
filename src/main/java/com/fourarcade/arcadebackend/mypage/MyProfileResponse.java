package com.fourarcade.arcadebackend.mypage;

import com.fourarcade.arcadebackend.user.User;
import lombok.Builder;
import lombok.Getter;

import java.util.UUID;

@Getter
@Builder
public class MyProfileResponse {

    private UUID id;
    private String email;
    private String nickname;
    private String profileImg;

    public static MyProfileResponse from(User user){
        return MyProfileResponse.builder()
                .id(user.getId())
                .email(user.getEmail())
                .nickname(user.getNickname())
                .profileImg(user.getProfileImg())
                .build();
    }
}
