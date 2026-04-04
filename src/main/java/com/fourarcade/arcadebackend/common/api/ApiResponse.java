package com.fourarcade.arcadebackend.common.api;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Builder;
import lombok.Getter;
import org.springframework.web.context.request.RequestAttributes;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import java.time.OffsetDateTime;

@Getter
@Builder

// null인 필드(성공 시 error 등)는 JSON 에서 제외
@JsonInclude(JsonInclude.Include.NON_NULL)
public class ApiResponse<T> {

    private final boolean success;      // 성공 여부
    private final T data;               // 성공 시 전달할 실제 데이터(실패 시 null)
    private final ApiError error;       // 실패 시 전달할 에러 상세 정보(성공 시 null)
    private final String serverTime;    // 응답이 나간 서버 시간
    private final String path;          // 이 응답을 발생시킨 API URI

    /*
    * 성공 시
    * return ApiResponse.ok(userDto);
    * */
    public static <T> ApiResponse<T> ok(T data) {
        return ApiResponse.<T>builder()
                .success(true)
                .data(data)     // 던져준 데이터
                .serverTime(OffsetDateTime.now().toString())    // 현재 시간
                .path(getCurrentPath()) // 요청 온 주소 자동 추적
                .build();
    }

    /*
    * 실패 시
    * 예외 시 GlobalExceptionHandler 가 잡아 클라이언트에게 보냄
    * return ApiResponse.fail(ApiError.of(...));
    * */
    public static <T> ApiResponse<T> fail(ApiError error) {
        return ApiResponse.<T>builder()
                .success(false)
                .error(error)   // 에러 객체
                .serverTime(OffsetDateTime.now().toString())    // 에러가 나온 시간
                .path(getCurrentPath()) // 에러 나온 주소 자동 추적
                .build();

    }

    public static ApiResponse<Void> ok() {
        return ApiResponse.<Void>builder()
                .success(true)
                .serverTime(OffsetDateTime.now().toString())
                .path(getCurrentPath())
                .build();
    }

    // Spring 컨텍스트에서 현재 요청의 URI 를 자동으로 가져오는 메서드
    private static String getCurrentPath() {
       RequestAttributes attributes = RequestContextHolder.getRequestAttributes();

       // 현재 요청이 웹 요청(Servlet)이 맞는지 확인 후 URI 추출
       if(attributes instanceof ServletRequestAttributes) {
           return ((ServletRequestAttributes) attributes).getRequest().getRequestURI();
       }

       // 웹 요청이 아니거나 못 찾으면 UNKNOWN 반환
       return "UNKNOWN";
    }
}
