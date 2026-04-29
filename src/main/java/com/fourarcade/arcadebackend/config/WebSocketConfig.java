package com.fourarcade.arcadebackend.config;

import com.fourarcade.arcadebackend.websocket.RoomWebSocketHandler;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.socket.config.annotation.EnableWebSocket;
import org.springframework.web.socket.config.annotation.WebSocketConfigurer;
import org.springframework.web.socket.config.annotation.WebSocketHandlerRegistry;

@Configuration
@EnableWebSocket
@RequiredArgsConstructor
public class WebSocketConfig implements WebSocketConfigurer {

    private final RoomWebSocketHandler roomWebSocketHandler;

    @Override
    public void registerWebSocketHandlers(WebSocketHandlerRegistry registry) {
        // "/ws/room" 주소로 들어오는 요청을 핸들러로 연결
        registry.addHandler(roomWebSocketHandler, "/ws/room")
                .setAllowedOrigins("*"); // CORS 에러 방지 (Postman, 로컬 프론트엔드 허용)
    }
}
