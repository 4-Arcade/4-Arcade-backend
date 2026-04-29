package com.fourarcade.arcadebackend.room;

import lombok.*;

import java.io.Serializable;
import java.util.List;
import java.util.UUID;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RoomRedisEntity implements Serializable {

    private UUID roomId;
    private String roomCode;
    private RoomStatus status;
    private UUID quizId;
    private String quizTitle;
    private RoomSettings settings;
    private List<Participant> participants;
    private GameProgress gameProgress;

    public enum RoomStatus {
        WAITING, READY, IN_GAME, RESULT
    }

    @Getter
    @Setter
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class Participant implements Serializable {
        private String nickname;
        private Boolean isHost;
        private Boolean isReady;
        private Boolean isConnected;
        private Long disconnectedAt; // 연결 끊긴 시간
        private int score;
    }

    @Getter @Setter @Builder @NoArgsConstructor @AllArgsConstructor
    public static class GameProgress implements Serializable {
        private int currentQuestionIndex;
        private int totalQuestionCount;
        private String questionStartedAt;   // ISO 8601 format
        private int timeLimit;
    }
}
