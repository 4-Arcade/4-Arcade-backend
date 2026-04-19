package com.fourarcade.arcadebackend.room.controller;

import com.fourarcade.arcadebackend.common.api.ApiResponse;
import com.fourarcade.arcadebackend.room.RoomCreateRequest;
import com.fourarcade.arcadebackend.room.RoomCreateResponse;
import com.fourarcade.arcadebackend.room.RoomService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/room")
@RequiredArgsConstructor
public class RoomController {

    private final RoomService roomService;

    @PostMapping
    public ResponseEntity<Object> createRoom(
            @Valid @RequestBody RoomCreateRequest request
    ) {
        // Service 에서 RoomException -> GlobalExceptionHandler 가 처리
        RoomCreateResponse response = roomService.createRoom(request);

        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.ok(response));
    }

}
