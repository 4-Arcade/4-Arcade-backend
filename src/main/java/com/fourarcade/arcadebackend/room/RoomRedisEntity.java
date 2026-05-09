package com.fourarcade.arcadebackend.room;

import lombok.*;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

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
        private int timeLimit;
        private long questionStartedAt;   // long(밀리초)로 변경

        // 정답자 체크용
        private boolean isQuestionSolved;

        // 문제 스냅샷
        private List<QuestionSnapshot> questions;

        // 유저별 게임 기록
        @Builder.Default
        private Map<String, PlayerGameData> players = new ConcurrentHashMap<>();
    }

    @Getter @Setter @Builder @NoArgsConstructor @AllArgsConstructor
    public static class QuestionSnapshot implements Serializable {
        private String videoId;
        private int startSec;
        private int endSec;
        private List<String> answer;
        private String hint;
    }

    @Getter @Setter @Builder @NoArgsConstructor @AllArgsConstructor
    public static class PlayerGameData implements Serializable {
        private int totalScore;             // 현재까지 총점
        private int currentWrongAttempts;   // 이번 문제에서 틀린 횟수
        @Builder.Default
        private List<QuestionResult> history = new ArrayList<>();   // 개인별 1번 문제 O, 2번 문제 X 기록
    }

    @Getter @Setter @Builder @NoArgsConstructor @AllArgsConstructor
    public static class QuestionResult implements Serializable {
        private int index;
        private boolean isCorrect;
        private int score;      // 이 문제에서 얻은 점수
        private String correctAnswer;
    }
}
