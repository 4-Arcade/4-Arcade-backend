package com.fourarcade.arcadebackend.websocket.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class ErrorPayload {
    private String code;  // "ROOM_NOT_FOUND", "NICKNAME_TAKEN"..
}
