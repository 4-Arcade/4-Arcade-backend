package com.fourarcade.arcadebackend.room.dto;

import com.fourarcade.arcadebackend.room.RoomRedisEntity;
import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

import java.util.UUID;

@Getter
@Setter
@Builder
public class RoomInfoResponse {
    private UUID roomId;
    private String roomCode;
    private RoomRedisEntity.RoomStatus status;
    private int currentPlayerCount;
    private int maxPlayerCount;
    private String quizTitle;
}
