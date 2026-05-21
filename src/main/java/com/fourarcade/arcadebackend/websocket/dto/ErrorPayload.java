package com.fourarcade.arcadebackend.websocket.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@AllArgsConstructor
@NoArgsConstructor
public class ErrorPayload {
    private String errorCode;  // "ROOM_NOT_FOUND", "NICKNAME_TAKEN"..
    private String message;

    // 기존 코드 호환용 생성자 (message 없는 경우)
    public ErrorPayload(String errorCode) {
        this.errorCode = errorCode;
        this.message = null;
    }
}
