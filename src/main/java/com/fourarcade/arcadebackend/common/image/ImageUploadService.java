package com.fourarcade.arcadebackend.common.image;

import com.fourarcade.arcadebackend.common.exception.BusinessException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;

import java.io.IOException;
import java.io.InputStream;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class ImageUploadService {

    private final S3Client s3Client;

    @Value("${cloud.aws.s3.bucket}")
    private String bucketName;

    @Value("${cloud.aws.s3.public-domain}")
    private String publicDomain;

    // 허용할 이미지 확장자
    private static final List<String> ALLOWED_EXTENSIONS = Arrays.asList("jpg", "jpeg", "png", "gif", "webp");
    // 파일 크기 제한 (5MB)
    private static final long MAX_FILE_SIZE = 5 * 1024 * 1024;

    public String uploadImage(MultipartFile file) {
        // 파일 비어있는지 확인
        if (file.isEmpty() || file.getOriginalFilename() == null) {
            throw new BusinessException("EMPTY_FILE", "업로드할 파일이 없습니다.", HttpStatus.BAD_REQUEST);
        }

        // 용량 제한 검증
        if (file.getSize() > MAX_FILE_SIZE) {
            throw new BusinessException("FILE_TOO_LARGE", "파일 크기는 5MB를 넘을 수 없습니다.", HttpStatus.BAD_REQUEST);
        }

        // 확장자 검증
        String originalFilename = file.getOriginalFilename();
        String extension = getExtension(originalFilename);
        if (!ALLOWED_EXTENSIONS.contains(extension.toLowerCase())) {
            throw new BusinessException("INVALID_EXTENSION", "허용되지 않는 파일 형식입니다. (jpg, png, gif, webp만 가능)", HttpStatus.BAD_REQUEST);
        }

        // 저장할 새 이름 생성 (UUID + 확장자)
        String S3FileName = UUID.randomUUID().toString() + "." + extension;

        // 업로드
        try (InputStream inputStream = file.getInputStream()) {
            PutObjectRequest putObjectRequest = PutObjectRequest.builder()
                    .bucket(bucketName)
                    .key(S3FileName)
                    .contentType(file.getContentType())
                    .contentLength(file.getSize())
                    .build();

            s3Client.putObject(putObjectRequest,
                    software.amazon.awssdk.core.sync.RequestBody.fromInputStream(inputStream, file.getSize()));    // 누구나 볼 수 있게 권한 부여
        } catch (IOException e) {
            log.error("Failed to upload image to S3: {}", e.getMessage());
            throw new BusinessException("UPLOAD_FAILED", "이미지 업로드에 실패했습니다.", HttpStatus.INTERNAL_SERVER_ERROR);
        }

        // 최종 URL 반환
        return publicDomain + "/" +S3FileName;
    }

    // 파일 이름에서 확장자만 가져오는 메서드
    private String getExtension(String filename) {
        int lastDotIndex = filename.lastIndexOf('.');
        if (lastDotIndex == -1) {
            throw new BusinessException("INVALID_FILE", "확장자가 없는 파일은 업로드할 수 없습니다.", HttpStatus.BAD_REQUEST);
        }
        return filename.substring(lastDotIndex + 1);
    }
}
