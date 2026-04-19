package com.fourarcade.arcadebackend.room;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.util.List;
import java.util.UUID;

@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RoomRedisEntity implements Serializable {

    private UUID roomId;
    private String roomCode;
    private RoomStatus status;  // "waiting", "playing", "finished"
    private UUID quizId;
    private String quizTitle;
    private RoomSettings settings;
    private List<Participant> participants;

    public enum RoomStatus {
        WAITING, PLAYING, FINISHED
    }

    @Getter
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class Participant implements Serializable {
        private String nickname;
        private Boolean isHost;
        private int score;
    }

}
