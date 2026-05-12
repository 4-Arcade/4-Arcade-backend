package com.fourarcade.arcadebackend.user.service;

import com.fourarcade.arcadebackend.common.exception.BusinessException;
import com.fourarcade.arcadebackend.common.image.ImageUploadService;
import com.fourarcade.arcadebackend.user.User;
import com.fourarcade.arcadebackend.user.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.util.UUID;

@Service
@RequiredArgsConstructor
public class UserImageService {

    // 이미지 업로드 도구 (common)
    private final ImageUploadService imageUploadService;
    // user DB
    private final UserRepository userRepository;

    @Transactional
    public String updateProfileImage(UUID userId, MultipartFile file) {
        // R2 클라우드에 사진 올리고 URL 받아옴
        String imageUrl = imageUploadService.uploadImage(file);

        // DB 에서 유저 찾기
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new BusinessException("USER_NOT_FOUND", "해당 유저를 찾을 수 없습니다.", HttpStatus.NOT_FOUND));

        // user entity 의 프로필 사진 url -> 방금 받아온 url 교체
        user.updateProfileImage(imageUrl);

        // url 반환
        return imageUrl;
    }
}
