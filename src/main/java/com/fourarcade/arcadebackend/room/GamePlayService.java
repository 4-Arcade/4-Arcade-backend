package com.fourarcade.arcadebackend.room;

import com.fourarcade.arcadebackend.question.Question;
import com.fourarcade.arcadebackend.question.QuestionRepository;
import com.fourarcade.arcadebackend.websocket.RoomWebSocketHandler;
import com.fourarcade.arcadebackend.websocket.dto.WsEvent;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Lazy;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.scheduling.TaskScheduler;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ScheduledFuture;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

@Slf4j
@Service
public class GamePlayService {

    private final RedisTemplate<String, Object> redisTemplate;
    private final TaskScheduler taskScheduler;  // 비동기 타이머용
    private final RoomWebSocketHandler webSocketHandler;  // 이벤트 전송용
    private final QuestionRepository questionRepository;

    public GamePlayService(
            RedisTemplate<String, Object> redisTemplate,
            TaskScheduler taskScheduler,
            @Lazy RoomWebSocketHandler webSocketHandler,
            QuestionRepository questionRepository
    ) {
        this.redisTemplate = redisTemplate;
        this.taskScheduler = taskScheduler;
        this.webSocketHandler = webSocketHandler;
        this.questionRepository = questionRepository;
    }

    // 초당 정답 제출 횟수 제한 (Rate Limiting): roomId:nickname -> 타임스탬프 리스트
    private final Map<String, List<Long>> answerRateLimits = new ConcurrentHashMap<>();
    // 현재 진행 중인 방의 문제 종료 타이머
    private final Map<String, ScheduledFuture<?>> roomTimers = new ConcurrentHashMap<>();

    private static final String ROOM_KEY_PREFIX = "room:";

    // 게임 시작 (game:start)
    public void startGame(String roomId, String hostNickname) {
        RoomRedisEntity room = (RoomRedisEntity) redisTemplate.opsForValue().get(ROOM_KEY_PREFIX + roomId);
        if (room == null) return;

        // 호스트 여부 및 상태 검증
        if (room.getStatus() != RoomRedisEntity.RoomStatus.READY) {
            webSocketHandler.sendError(roomId, hostNickname, "INVALID_STATE", "READY 상태에서만 시작할 수 있습니다.");
            return;
        }

        long connectedPlayers = room.getParticipants().stream().filter(RoomRedisEntity.Participant::getIsConnected).count();
        if (connectedPlayers < 2) {
            webSocketHandler.sendError(roomId, hostNickname, "NOT_ENOUGH_PLAYERS", "최소 2명 이상 접속해야 시작 가능합니다.");
            return;
        }

        // DB 에서 퀴즈 조회 및 스냅샷 생성
        List<Question> dbQuestions = questionRepository.findByQuiz_IdOrderByOrderIndexAsc(room.getQuizId());

        if (dbQuestions.isEmpty()) {
            webSocketHandler.sendError(roomId, hostNickname, "NO_QUESTIONS", "해당 퀴즈에 등록된 문제가 없습니다.");
            return;
        }

        // DB 엔티티를 Redis 스냅샷으로 변환 (videoId 파싱 포함)
        List<RoomRedisEntity.QuestionSnapshot> snapshots = dbQuestions.stream()
                .map(q -> RoomRedisEntity.QuestionSnapshot.builder()
                        .videoId(extractVideoId(q.getYoutubeUrl())) // 파싱 메서드 호출
                        .startSec(q.getStartSec())
                        .endSec(q.getEndSec())
                        .answer(Arrays.asList(q.getAnswers()))  // String[] -> List<String> 변환
                        .hint(q.getHint())
                        .build())
                .collect(Collectors.toList());

        // gameProgress 초기화
        RoomRedisEntity.GameProgress progress = new RoomRedisEntity.GameProgress();
        progress.setQuestions(snapshots);
        progress.setCurrentQuestionIndex(0);
        progress.setTotalQuestionCount(snapshots.size());   // 전체 문제 수
        progress.setTimeLimit(room.getSettings().getTimeLimit());

        for (RoomRedisEntity.Participant p : room.getParticipants()) {
            if (p.getIsConnected()) {
                progress.getPlayers().put(p.getNickname(), new RoomRedisEntity.PlayerGameData());
            }
        }
        room.setGameProgress(progress);
        room.setStatus(RoomRedisEntity.RoomStatus.IN_GAME);
        redisTemplate.opsForValue().set(ROOM_KEY_PREFIX + roomId, room);

        // 카운트다운 시작
        startCountdown(roomId, 3);
    }

    // 카운트다운 (game:countdown)
    private void startCountdown(String roomId, int count) {
        if (count > 0) {
            webSocketHandler.broadcastToRoom(roomId, WsEvent.builder().event("game:countdown").data(Map.of("count", count)).build(), null);

            // 1초 뒤에 다음 카운트 실행
            taskScheduler.schedule(() -> startCountdown(roomId, count - 1),
                    Instant.now().plusSeconds(1));
        } else {
            // 카운트다운 종료 -> 첫 번째 문제 시작
            startQuestion(roomId);
        }
    }

    // 문제 시작 (question:start & question:media)
    private void startQuestion(String roomId) {

        RoomRedisEntity room = getValidRoomWithProgress(roomId);
        if (room == null) return;

        RoomRedisEntity.GameProgress progress = room.getGameProgress();
        RoomRedisEntity.QuestionSnapshot currentQ = progress.getQuestions().get(progress.getCurrentQuestionIndex());

        // 정답 여부 초기화
        progress.setQuestionSolved(false);

        // 오답 횟수 초기화
        progress.getPlayers().values().forEach(p -> p.setCurrentWrongAttempts(0));
        progress.setQuestionStartedAt(System.currentTimeMillis());  // 문제 시작 시간 기록
        redisTemplate.opsForValue().set(ROOM_KEY_PREFIX + roomId, room);

        // question:start (videoId 없음)
        Map<String, Object> startData = new HashMap<>();
        startData.put("index", progress.getCurrentQuestionIndex() + 1);
        startData.put("totalCount", progress.getQuestions().size());
        startData.put("timeLimit", room.getSettings().getTimeLimit());
        startData.put("hint", currentQ.getHint());

        webSocketHandler.broadcastToRoom(roomId, WsEvent.builder()
                .event("question:start")
                .data(startData)
                .build(), null);

        // 직후 question:media (videoId 포함)
        Map<String, Object> mediaData = Map.of(
                "videoId", currentQ.getVideoId(),
                "startSec", currentQ.getStartSec(),
                "endSec", currentQ.getEndSec()
        );
        webSocketHandler.broadcastToRoom(roomId, WsEvent.builder().event("question:media").data(mediaData).build(), null);

        // 타임아웃 타이머 예약 (timeLimit 초 뒤에 강제 종료)
        ScheduledFuture<?> timer = taskScheduler.schedule(() -> endQuestion(roomId),
                Instant.now().plusSeconds(room.getSettings().getTimeLimit()));
        roomTimers.put(roomId, timer);
    }

    // 정답 입력 처리 (game:answer)
    public void processAnswer(String roomId, String nickname, String answer) {
        // Rate Limiting (초당 5회)
        String limitKey = roomId + ":" + nickname;
        long now = System.currentTimeMillis();
        List<Long> timestamps = answerRateLimits.computeIfAbsent(limitKey, k -> new ArrayList<>());
        timestamps.removeIf(t -> now - t > 1000);   // 1초 지난 기록 삭제

        if (timestamps.size() >= 5) return; // 무시
        timestamps.add(now);

        RoomRedisEntity room = getValidRoomWithProgress(roomId);
        if (room == null) return;

        RoomRedisEntity.GameProgress progress = room.getGameProgress();
        RoomRedisEntity.QuestionSnapshot currentQ = progress.getQuestions().get(progress.getCurrentQuestionIndex());
        RoomRedisEntity.PlayerGameData playerData = progress.getPlayers().get(nickname);

        // 이미 정답자가 나왔거나 오답 초과면 무시
        if (playerData == null || progress.isQuestionSolved()) return;

        // 다중 정답 검사 로직
        boolean isCorrect = currentQ.getAnswer().stream()
                .anyMatch(correctAnswer -> correctAnswer.trim().equalsIgnoreCase(answer.trim()));

        if (!isCorrect) {
            // 오답 처리
            playerData.setCurrentWrongAttempts(playerData.getCurrentWrongAttempts() + 1);

            Integer wrongLimit = room.getSettings().getWrongAnswerLimit();
            if (wrongLimit != null && playerData.getCurrentWrongAttempts() >= wrongLimit) {
                // answer:locked 개별 전송
                webSocketHandler.sendToPlayer(roomId, nickname, WsEvent.builder()
                        .event("answer:locked")
                        .data(Map.of("questionIndex", progress.getCurrentQuestionIndex() + 1, "message", "이미 오답을 사용했습니다"))
                        .build());
            }
            redisTemplate.opsForValue().set(ROOM_KEY_PREFIX + roomId, room);
            return;
        }

        // 정답 처리
        progress.setQuestionSolved(true);

        // 기존 타이머 취소
        ScheduledFuture<?> currentTimer = roomTimers.remove(roomId);
        if (currentTimer != null) currentTimer.cancel(false);

        // 점수 계산
        int earnedScore = 1000;
        playerData.setTotalScore(playerData.getTotalScore() + earnedScore);

        // 결과 기록
        playerData.getHistory().add(RoomRedisEntity.QuestionResult.builder()
                .index(progress.getCurrentQuestionIndex() + 1)
                .isCorrect(true)
                .score(earnedScore)
                .correctAnswer(currentQ.getAnswer().get(0))
                .build());

        redisTemplate.opsForValue().set(ROOM_KEY_PREFIX + roomId, room);

        long elapsedTime = (System.currentTimeMillis() - progress.getQuestionStartedAt()) / 1000;
        int timeLeft = Math.max(0, room.getSettings().getTimeLimit() - (int) elapsedTime);

        // question:correct 발송
        webSocketHandler.broadcastToRoom(roomId, WsEvent.builder()
                .event("question:correct")
                .data(Map.of("nickname", nickname, "score", earnedScore, "timeLeft", timeLeft))
                .build(), null);

        // 3초 뒤에 문제 완전 종료 처리
        taskScheduler.schedule(() -> endQuestion(roomId), Instant.now().plusSeconds(3));
    }

    // 문제 종료 (question:end)
    private void endQuestion(String roomId) {
        roomTimers.remove(roomId);  // 아무도 못 맞추고 끝났을 때를 대비해 타이머 맵에서 무조건 비워주기

        RoomRedisEntity room = getValidRoomWithProgress(roomId);
        if (room == null) return;

        RoomRedisEntity.GameProgress progress = room.getGameProgress();
        RoomRedisEntity.QuestionSnapshot currentQ = progress.getQuestions().get(progress.getCurrentQuestionIndex());

        // 정답 못 맞춘 사람들 기록 처리
        for (Map.Entry<String, RoomRedisEntity.PlayerGameData> entry : progress.getPlayers().entrySet()) {
            RoomRedisEntity.PlayerGameData pd = entry.getValue();
            boolean hasCorrected = pd.getHistory().stream().anyMatch(h -> h.getIndex() == progress.getCurrentQuestionIndex() + 1);
            if (!hasCorrected) {
                pd.getHistory().add(RoomRedisEntity.QuestionResult.builder()
                        .index(progress.getCurrentQuestionIndex() + 1)
                        .isCorrect(false)
                        .score(0)
                        .correctAnswer(currentQ.getAnswer().get(0))
                        .build());
            }
        }
        redisTemplate.opsForValue().set(ROOM_KEY_PREFIX + roomId, room);

        // 스코어보드 생성
        List<Map<String, Object>> scores = progress.getPlayers().entrySet().stream()
                .map(e -> Map.<String, Object>of(
                        "nickname", e.getKey(),
                        "questionScore", e.getValue().getHistory().get(e.getValue().getHistory().size() - 1).getScore(),
                        "totalScore", e.getValue().getTotalScore()
                ))
                .collect(Collectors.toList());

        Map<String, Object> endData = new HashMap<>();
        endData.put("correctAnswer", room.getSettings().getShowAnswer() ? currentQ.getAnswer().get(0) : null);
        endData.put("scores", scores);

        webSocketHandler.broadcastToRoom(roomId, WsEvent.builder().event("question:end").data(endData).build(), null);

        // 다음 진행 결정 (다음 문제 or 최종 결과)
        if (progress.getCurrentQuestionIndex() + 1 < progress.getQuestions().size()) {
            progress.setCurrentQuestionIndex(progress.getCurrentQuestionIndex() + 1);
            redisTemplate.opsForValue().set(ROOM_KEY_PREFIX + roomId, room);
            // 5초 뒤 다음 문제 카운트다운 시작
            taskScheduler.schedule(() -> startCountdown(roomId, 3), Instant.now().plusSeconds(5));
        } else {
            // 게임 끝 -> 결과 화면 전송
            room.setStatus(RoomRedisEntity.RoomStatus.RESULT);
            redisTemplate.opsForValue().set(ROOM_KEY_PREFIX + roomId, room);
            sendGameResult(room);
        }
    }

    // 결과 화면 (game:result) - 개인화 전송
    private void sendGameResult(RoomRedisEntity room) {
        RoomRedisEntity.GameProgress progress = room.getGameProgress();

        // 랭킹 정렬
        List<Map<String, Object>> baseRanking = progress.getPlayers().entrySet().stream()
                .sorted((e1, e2) -> Integer.compare(e2.getValue().getTotalScore(), e1.getValue().getTotalScore()))
                .map(e -> {
                    Map<String, Object> r = new HashMap<>();
                    r.put("nickname", e.getKey());
                    r.put("totalScore", e.getValue().getTotalScore());
                    return r;
                })
                .toList();

        // 등수 부여
        for (int i = 0; i < baseRanking.size(); i++) {
            baseRanking.get(i).put("rank", i + 1);
        }

        // 각 유저별로 개인화(is Me, MyQuestions)하여 전송
        for (RoomRedisEntity.Participant p : room.getParticipants()) {
            if (p.getIsConnected() && progress.getPlayers().containsKey(p.getNickname())) {
                // 랭킹에 isMe 주입
                List<Map<String, Object>> personalRanking = new ArrayList<>();
                for (Map<String, Object> rankObj : baseRanking) {
                    Map<String, Object> newRankObj = new HashMap<>(rankObj);
                    newRankObj.put("isMe", p.getNickname().equals(rankObj.get("nickname")));
                    personalRanking.add(newRankObj);
                }

                Map<String, Object> resultData = Map.of(
                        "ranking", personalRanking,
                        "myQuestions", progress.getPlayers().get(p.getNickname()).getHistory()
                );

                webSocketHandler.sendToPlayer(room.getRoomId().toString(), p.getNickname(),
                        WsEvent.builder().event("game:result").data(resultData).build());
            }
        }
    }

    // Youtube URL 에서 VideoId 만 뽑아내는 메서드
    private String extractVideoId(String url) {
        if (url == null) return null;

        String regex = "(?:youtube\\.com/(?:[^/]+/.+/|(?:v|e(?:mbed)?)/|.*[?&]v=)|youtu\\.be/)([^\"&?/\\s]{11})";
        Matcher matcher = Pattern.compile(regex).matcher(url);
        if (matcher.find()) {
            return matcher.group(1);    // 11자리 비디오 ID 반환
        }
        return null;    // 파싱 실패 시 원본 반환
    }

    // 중복 제거용 공통 헬퍼 메서드
    private RoomRedisEntity getValidRoomWithProgress(String roomId) {
        RoomRedisEntity room = (RoomRedisEntity) redisTemplate.opsForValue().get(ROOM_KEY_PREFIX + roomId);

        // 방이 없거나 게임 진행 데이터가 없으면 에러 없이 조용히 종료
        if (room == null || room.getGameProgress() == null || room.getGameProgress().getQuestions() == null) {
            return null;
        }
        return room;
    }

}
