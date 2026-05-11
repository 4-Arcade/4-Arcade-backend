package com.fourarcade.arcadebackend.auth;

import com.fourarcade.arcadebackend.auth.dto.*;
import com.fourarcade.arcadebackend.common.api.ApiResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;

    // 회원가입
    @PostMapping("/register")
    public ResponseEntity<ApiResponse<AuthResponse>> register(
            @Valid @RequestBody RegisterRequest request) {

        AuthResponse authResponse = authService.register(request);

        // 201 응답 및 통일된 ApiResponse 포맷 적용
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.ok(authResponse));
    }

    // 로그인
    @PostMapping("/login")
    public ResponseEntity<ApiResponse<AuthResponse>> login(
            @Valid @RequestBody LoginRequest request) {

        // 로그인 수행
        AuthResponse authResponse = authService.login(request);

        return ResponseEntity.ok(ApiResponse.ok(authResponse));
    }

    // AccessToken 재발급
    @PostMapping("/refresh")
    public ResponseEntity<ApiResponse<AuthResponse>> refresh(
            @Valid @RequestBody RefreshRequest request) {

        // 서비스 로직에서 토큰 재발급
        AuthResponse authResponse = authService.refresh(request.getRefreshToken());

        return ResponseEntity.ok(ApiResponse.ok(AuthResponse.withoutRefreshToken(authResponse)));
    }

    @DeleteMapping("/logout")
    public ApiResponse<Void> logout(@RequestBody LogoutRequest request) {
        authService.logout(request.getRefreshToken());

        // 데이터 없는 성공 응답 (success: true, data: null)
        return ApiResponse.ok();
    }
}
