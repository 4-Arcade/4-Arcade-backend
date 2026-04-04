package com.fourarcade.arcadebackend.common.exception;

import com.fourarcade.arcadebackend.common.api.ApiError;
import com.fourarcade.arcadebackend.common.api.ApiResponse;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice
public class GlobalExceptionHandler {

    // @Valid 유효성 검사 실패
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ApiResponse<Void>> handleMethodArgumentNotValidException(MethodArgumentNotValidException ex) {
        ApiError error = ApiError.ofValidation(ex.getBindingResult());
        return ResponseEntity.badRequest().body(ApiResponse.fail(error));
    }

    // 그 외 예외
    @ExceptionHandler(Exception.class)
    public  ResponseEntity<ApiResponse<Void>> handleException(Exception ex) {
        ApiError error = ApiError.of("INTERNAL_SERVER_ERROR", "서버 오류가 발생했습니다.", 500);
        return ResponseEntity.internalServerError().body(ApiResponse.fail(error));
    }
}
