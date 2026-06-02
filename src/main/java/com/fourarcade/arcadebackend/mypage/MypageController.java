package com.fourarcade.arcadebackend.mypage;

import com.fourarcade.arcadebackend.common.api.ApiResponse;
import com.fourarcade.arcadebackend.common.security.CustomUserPrincipal;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/mypage")
@RequiredArgsConstructor
public class MypageController {

    private final MypageService mypageService;

    @GetMapping("/quiz")
    public ResponseEntity<ApiResponse<MyQuizListResponse>> getMyQuizzes(
            @AuthenticationPrincipal CustomUserPrincipal principal,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "7") int size,
            @RequestParam(required = false) String keyword
    ) {
        MyQuizListResponse response =
                mypageService.getMyQuizzes(principal.getUserId(), page, size, keyword);

        return ResponseEntity.ok(ApiResponse.ok(response));
    }

    @PatchMapping("/nickname")
    public ResponseEntity<ApiResponse<MyProfileResponse>> updateNickname(
            @AuthenticationPrincipal CustomUserPrincipal principal,
            @Valid @RequestBody NicknameUpdateRequest request
    ){
        MyProfileResponse response =
                mypageService.updateNickname(principal.getUserId(),request);

        return ResponseEntity.ok(ApiResponse.ok(response));
    }
}