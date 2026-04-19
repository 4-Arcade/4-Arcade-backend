package com.fourarcade.arcadebackend.common.exception;

import lombok.Getter;
import org.springframework.http.HttpStatus;

@Getter
public class RoomException extends RuntimeException {
    private final String code;
    private final HttpStatus status;

    public RoomException(String code, String message, HttpStatus status) {
        super(message);
        this.code = code;
        this.status = status;
    }
}
