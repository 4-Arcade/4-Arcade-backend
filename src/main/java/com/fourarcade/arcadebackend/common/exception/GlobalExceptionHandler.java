package com.fourarcade.arcadebackend.common.exception;

import com.fourarcade.arcadebackend.common.api.ApiError;
import com.fourarcade.arcadebackend.common.api.ApiResponse;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.http.converter.HttpMessageNotReadableException;

@RestControllerAdvice
public class GlobalExceptionHandler {

    // @Valid 유효성 검사 실패
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ApiResponse<Void>> handleMethodArgumentNotValidException(MethodArgumentNotValidException ex) {
        ApiError error = ApiError.ofValidation(ex.getBindingResult());
        return ResponseEntity.badRequest().body(ApiResponse.fail(error));
    }

    // 인증 예외 (401)
    @ExceptionHandler(AuthException.class)
    public ResponseEntity<ApiResponse<Void>> handleAuthException(AuthException ex) {
        // 예외에 담아둔 코드와 메세지를 그대로 ApiError 로 변환
        ApiError error = ApiError.of(ex.getCode(), ex.getMessage(), ex.getStatus().value());
        return ResponseEntity.status(ex.getStatus()).body(ApiResponse.fail(error));
    }

    @ExceptionHandler(BusinessException.class)
    public ResponseEntity<ApiResponse<Void>> handleBusinessException(BusinessException ex) {
        ApiError error = ApiError.of(ex.getCode(), ex.getMessage(), ex.getStatus().value());
        return ResponseEntity.status(ex.getStatus()).body(ApiResponse.fail(error));
    }

    // Room 생성 예외
    @ExceptionHandler(RoomException.class)
    public ResponseEntity<ApiResponse<Void>> handleRoomException(RoomException ex) {
        ApiError error = ApiError.of(ex.getCode(), ex.getMessage(), ex.getStatus().value());
        return ResponseEntity.status(ex.getStatus()).body(ApiResponse.fail(error));
    }

    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<ApiResponse<Void>> handleHttpMessageNotReadableException(HttpMessageNotReadableException ex) {
        ApiError error = ApiError.of(
                "VALIDATION_FAILED",
                "카테고리를 선택하지 않았거나 올바르지 않은 값입니다.",
                400
        );

        return ResponseEntity.badRequest().body(ApiResponse.fail(error));
    }

    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<ApiResponse<Void>> handleIllegalArgumentException(IllegalArgumentException ex) {
        ApiError error = ApiError.of("VALIDATION_FAILED", ex.getMessage(), 400);
        return ResponseEntity.badRequest().body(ApiResponse.fail(error));
    }

    // 그 외 예외
    @ExceptionHandler(Exception.class)
    public  ResponseEntity<ApiResponse<Void>> handleException(Exception ex) {
        ApiError error = ApiError.of("INTERNAL_SERVER_ERROR", "서버 오류가 발생했습니다.", 500);
        return ResponseEntity.internalServerError().body(ApiResponse.fail(error));
    }
}
