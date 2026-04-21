package com.fourarcade.arcadebackend.room.dto;

import com.fourarcade.arcadebackend.room.RoomRedisEntity;
import com.fourarcade.arcadebackend.room.RoomSettings;
import lombok.Builder;
import lombok.Getter;

import java.util.UUID;

@Getter
@Builder
public class RoomCreateResponse {

    private UUID roomId;
    private String roomCode;
    private RoomRedisEntity.RoomStatus status;
    private String quizTitle;
    private RoomSettings settings;

}
