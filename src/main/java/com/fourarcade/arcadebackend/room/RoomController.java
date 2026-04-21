package com.fourarcade.arcadebackend.room;

import com.fourarcade.arcadebackend.common.api.ApiResponse;
import com.fourarcade.arcadebackend.room.dto.RoomCreateRequest;
import com.fourarcade.arcadebackend.room.dto.RoomCreateResponse;
import com.fourarcade.arcadebackend.room.dto.RoomInfoResponse;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/room")
@RequiredArgsConstructor
public class RoomController {

    private final RoomService roomService;

    // 방 생성
    @PostMapping
    public ResponseEntity<Object> createRoom(
            @Valid @RequestBody RoomCreateRequest request
    ) {
        // Service 에서 RoomException -> GlobalExceptionHandler 가 처리
        RoomCreateResponse response = roomService.createRoom(request);

        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.ok(response));
    }

    // 방 조회
    @GetMapping("/{roomCode}")
    public ResponseEntity<ApiResponse<RoomInfoResponse>> getRoomInfo(
            @PathVariable String roomCode,
            HttpServletRequest request) {

        // 요청을 보낸 클라이언트의 IP 추출
        String clientIp = getClientIp(request);

        RoomInfoResponse response = roomService.getRoomInfo(roomCode, clientIp);

        return ResponseEntity.ok(ApiResponse.ok(response));
    }

    // IP 추출용 메서드
    private String getClientIp(HttpServletRequest request) {
        String ip = request.getHeader("x-forwarded-for");
        if (ip == null || ip.isEmpty() || "unknown".equalsIgnoreCase(ip)) {
            ip = request.getRemoteAddr();
        }
        // 다중 프록시를 거쳤을 경우 첫 번째 IP가 실제 클라이언트 IP
        return ip.split(",")[0].trim();
    }

}
