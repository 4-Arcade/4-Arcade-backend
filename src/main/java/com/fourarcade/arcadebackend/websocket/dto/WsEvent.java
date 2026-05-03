package com.fourarcade.arcadebackend.websocket.dto;

import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class WsEvent<T> {
    private String event;
    private T data;
}
