package com.fourarcade.arcadebackend.auth;

import com.fourarcade.arcadebackend.common.api.ApiResponse;
import com.fourarcade.arcadebackend.common.exception.AuthException;
import com.fourarcade.arcadebackend.common.security.JwtTokenProvider;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseCookie;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.Duration;

@RestController
@RequestMapping("/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;

    // 회원가입
    @PostMapping("/register")
    public ResponseEntity<ApiResponse<AuthResponse>> register(
            @Valid @RequestBody RegisterRequest request,
            HttpServletResponse res) {

        AuthResponse authResponse = authService.register(request);
        addRefreshCookie(res, authResponse.getRefreshToken());

        // 201 응답 및 통일된 ApiResponse 포맷 적용
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.ok(AuthResponse.withoutRefreshToken(authResponse)));
    }

    // 로그인
    @PostMapping("/login")
    public ResponseEntity<ApiResponse<AuthResponse>> login(
            @Valid @RequestBody LoginRequest request,
            HttpServletResponse res) {

        AuthResponse authResponse = authService.login(request);
        addRefreshCookie(res, authResponse.getRefreshToken());
        return ResponseEntity.ok(ApiResponse.ok(AuthResponse.withoutRefreshToken(authResponse)));
    }

    // AccessToken 재발급
    @PostMapping("/refresh")
    public ResponseEntity<ApiResponse<AuthResponse>> refresh(
            @CookieValue(value = "refreshToken", required = false) String refreshToken,
            HttpServletResponse res) {

        if (refreshToken == null) {
            throw new AuthException("REFRESH_TOKEN_MISSING", "RefreshToken이 없습니다.", HttpStatus.BAD_REQUEST);
        }

        // 서비스 로직에서 토큰 재발급
        AuthResponse authResponse = authService.refresh(refreshToken);

        addRefreshCookie(res, authResponse.getRefreshToken());

        return ResponseEntity.ok(ApiResponse.ok(AuthResponse.withoutRefreshToken(authResponse)));
    }

    // RefreshToken 쿠키 설정
    private void addRefreshCookie(HttpServletResponse res, String refreshToken) {
        ResponseCookie cookie = ResponseCookie.from("refreshToken", refreshToken).httpOnly(true)           // JS 접근 차단
                .secure(true)             // HTTPS에서만 전송
                .path("/auth/refresh")    // 재발급 엔드포인트에서만 전송
                .maxAge(Duration.ofDays(30))
                .sameSite("Strict")
                .build();
        res.addHeader(HttpHeaders.SET_COOKIE, cookie.toString());
    }
}
