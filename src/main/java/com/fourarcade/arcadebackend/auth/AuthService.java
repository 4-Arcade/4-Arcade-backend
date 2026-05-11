package com.fourarcade.arcadebackend.auth;

import com.fourarcade.arcadebackend.auth.dto.AuthResponse;
import com.fourarcade.arcadebackend.auth.dto.LoginRequest;
import com.fourarcade.arcadebackend.auth.dto.RegisterRequest;
import com.fourarcade.arcadebackend.common.exception.AuthException;
import com.fourarcade.arcadebackend.common.security.JwtTokenProvider;
import com.fourarcade.arcadebackend.user.User;
import com.fourarcade.arcadebackend.user.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
public class AuthService {

    private final UserRepository userRepository;
    private final RefreshTokenRepository refreshTokenRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtTokenProvider jwtTokenProvider;

    private static final long REFRESH_TTL_DAYS = 30;

    @Transactional
    public AuthResponse register(RegisterRequest request) {

        // 이메일 중복 체크
        if (userRepository.existsByEmail(request.getEmail())) {
            throw new AuthException("EMAIL_DUPLICATED", "이미 가입된 이메일입니다.", HttpStatus.CONFLICT);
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

        return issueTokensAndRespond(newUser);
    }

    @Transactional
    public AuthResponse login(LoginRequest request) {
        // 유저 조회
        User user = userRepository.findByEmail(request.getEmail())
                .orElseThrow(() -> new AuthException("INVALID_CREDENTIALS", "이메일 또는 비밀번호가 일치하지 않습니다.", HttpStatus.UNAUTHORIZED));

        // 비밀번호 검증
        if (!passwordEncoder.matches(request.getPassword(), user.getPasswordHash())) {
            throw new AuthException("INVALID_CREDENTIALS", "이메일 또는 비밀번호가 일치하지 않습니다.", HttpStatus.UNAUTHORIZED);
        }

        // 탈퇴한 계정인지
        if (user.getDeletedAt() != null) {
            throw new AuthException("ACCOUNT_DELETED", "탈퇴한 계정입니다.", HttpStatus.FORBIDDEN);
        }

        return issueTokensAndRespond(user);
    }

    private AuthResponse issueTokensAndRespond(User user) {

        // Access Token 발급
        String accessToken = jwtTokenProvider.createAccessToken(
                user.getId(),
                user.getEmail(),
                List.of()
        );

        // RefreshToken 발급 (원문 + 해시)
        JwtTokenProvider.RefreshTokenPair refreshPair = jwtTokenProvider.createRefreshToken();
        String refreshForClient = refreshPair.tokenForClient();
        byte[] refreshHash = refreshPair.tokenHash();

        // DB 에는 해시만 저장
        refreshTokenRepository.save(RefreshToken.builder()
                .userId(user.getId())
                .tokenHash(refreshHash)
                .expiresAt(LocalDateTime.now().plusDays(REFRESH_TTL_DAYS))
                .build());

        return AuthResponse.of(accessToken, refreshForClient, user);
    }

    @Transactional
    public AuthResponse refresh(String refreshTokenForClient) {

        // 클라이언트 토큰 -> 해시
        byte[] hash = jwtTokenProvider.hashRefreshToken(refreshTokenForClient);

        // DB 에서 해시 조회
        RefreshToken tokenEntity = refreshTokenRepository.findByTokenHashAndRevokedAtIsNull(hash)
                .orElseThrow(() -> new AuthException("INVALID_REFRESH_TOKEN", "유효하지 않은 토큰입니다.", HttpStatus.UNAUTHORIZED));

        // 만료 확인
        if (tokenEntity.getExpiresAt().isBefore(LocalDateTime.now())) {
            throw new AuthException("EXPIRED_REFRESH_TOKEN", "만료된 토큰입니다.", HttpStatus.UNAUTHORIZED);
        }

        // 유저 조회
        User user = userRepository.findById(tokenEntity.getUserId())
                .orElseThrow(() -> new AuthException("USER_NOT_FOUND", "유저를 찾을 수 없습니다.", HttpStatus.NOT_FOUND));

        // 사용이 끝난 기존 리프레시 토큰은 DB 에서 즉시 삭제 (또는 폐기 처리)
        refreshTokenRepository.delete(tokenEntity);

        //  공통 메서드를 호출하여 새 Access + 새 Refresh 발급
        return issueTokensAndRespond(user);
    }

    @Transactional
    public void logout(String refreshTokenForClient) {
        // 클라이언트가 보낸 원문 토큰 해시로 변환
        byte[] hash = jwtTokenProvider.hashRefreshToken(refreshTokenForClient);

        // 해시값으로 DB 조회
        RefreshToken tokenEntity = refreshTokenRepository.findByTokenHashAndRevokedAtIsNull(hash)
                .orElseThrow(() -> new AuthException("INVALID_REFRESH_TOKEN", "유효하지 않은 토큰입니다.", HttpStatus.UNAUTHORIZED));

        // 토큰 삭제 (or 만료 처리)
        refreshTokenRepository.delete(tokenEntity);
    }
}
