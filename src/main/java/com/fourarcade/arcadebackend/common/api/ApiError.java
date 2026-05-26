package com.fourarcade.arcadebackend.common.api;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Builder;
import lombok.Getter;
import org.springframework.validation.BindingResult;

import java.util.List;
import java.util.Map;

@Getter
@Builder
@JsonInclude(JsonInclude.Include.NON_NULL)
public class ApiError {

    private final String code;      // 팀 표준 에러코드
    private final String message;   // 사용자 노출 메세지
    private final Integer status;   // HTTP Status
    private final List<Map<String, String>> details;    // 구체적 에러 항목들

    // 일반적인 예외 시
    public static ApiError of(String code, String message, int status) {
        return ApiError.builder()
                .code(code)
                .message(message)
                .status(status)
                .build();
    }

    // 유효성 검사(@Valid) 실패 시
    public static ApiError ofValidation(BindingResult bindingResult) {
        // 에러 목록을 [{"field": "email", "message": "이메일 형식이 아닙니다"}] 형태로 변환
        List<Map<String, String>> details = bindingResult.getFieldErrors().stream()
                .map(error -> Map.of("field", error.getField(), "message", error.getDefaultMessage()))
                .toList();

        String firstMessage = details.isEmpty()
                ? "입력값이 올바르지 않습니다."
                : details.get(0).get("message");

        return ApiError.builder()
                .code("VALIDATION_FAILED")
                .message(firstMessage)
                .status(400)
                .details(details)
                .build();
    }

}
