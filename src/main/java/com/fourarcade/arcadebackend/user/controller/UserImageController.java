package com.fourarcade.arcadebackend.user.controller;

import com.fourarcade.arcadebackend.common.api.ApiResponse;
import com.fourarcade.arcadebackend.common.security.CustomUserPrincipal;
import com.fourarcade.arcadebackend.user.service.UserImageService;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping("/api/users")
@RequiredArgsConstructor
public class UserImageController {

    private final UserImageService userImageService;

    /*
     * 유저 프로필 이미지 업로드 및 변경 API
     * POST /api/users/me/profile-image
     */
    @PatchMapping("/me/profile-image")
    public ResponseEntity<ApiResponse<String>> updateProfileImage(
            @AuthenticationPrincipal CustomUserPrincipal principal,
            @RequestPart("image") MultipartFile file
    ) {
        String newImageUrl = userImageService.updateProfileImage(principal.getUserId(), file);
        return ResponseEntity.ok(ApiResponse.ok(newImageUrl));
    }
}
