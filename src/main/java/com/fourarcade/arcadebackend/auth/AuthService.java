package com.fourarcade.arcadebackend.auth;

import com.fourarcade.arcadebackend.common.security.JwtTokenProvider;
import com.fourarcade.arcadebackend.user.User;
import com.fourarcade.arcadebackend.user.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class AuthService {
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtTokenProvider jwtTokenProvider;

    @Transactional
    public AuthResponse register(RegisterRequest request) {
        // 이메일 중복 체크
        if(userRepository.existsByEmail(request.getEmail())) {
            throw new IllegalArgumentException("Email address already in use");
        }

        // 비밀번호 암호화
        String encodePassword = passwordEncoder.encode(request.getPassword());

        // 유저 저장
        User newUser = User.builder()
                .email(request.getEmail())
                .passwordHash(encodePassword)
                .nickname(request.getNickname())
                .build();

        userRepository.save(newUser);

        // JWT 토큰 발급
        // Access Token 발급
        String accessToken = jwtTokenProvider.createAccessToken(
                newUser.getId(),
                newUser.getEmail(),
                List.of()
        );

        // 응답 객체 반환
        return AuthResponse.of(accessToken, newUser);
    }
}
