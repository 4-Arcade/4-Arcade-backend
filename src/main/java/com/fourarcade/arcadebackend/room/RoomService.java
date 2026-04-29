package com.fourarcade.arcadebackend.room;

import com.fourarcade.arcadebackend.common.exception.RoomException;
import com.fourarcade.arcadebackend.quiz.Quiz;
import com.fourarcade.arcadebackend.quiz.QuizService;
import com.fourarcade.arcadebackend.room.dto.RoomCreateRequest;
import com.fourarcade.arcadebackend.room.dto.RoomCreateResponse;
import com.fourarcade.arcadebackend.room.dto.RoomInfoResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

@Service
@RequiredArgsConstructor
public class RoomService {

    private final QuizService quizService;
    private final RoomCodeGenerator roomCodeGenerator;
    // Redis 설정 필요
    private final RedisTemplate<String, Object> redisTemplate;

    private static final String ROOM_KEY_PREFIX = "room:";
    private static final String CODE_KEY_PREFIX = "room:code:";

    private static final int MAX_PLAYERS = 8;

    public RoomCreateResponse createRoom(RoomCreateRequest request) {

        // 퀴즈 유효성 검증
        Quiz quiz = quizService.getQuizById(request.getQuizId());

        if (!quiz.isPublic()) {
            throw new RoomException("QUIZ_NOT_PUBLIC", "비공개 퀴즈입니다.", HttpStatus.BAD_REQUEST);
        }
        if (quiz.getQuestionCount() < 5) {
            throw new RoomException("QUIZ_MIN_QUESTIONS", "퀴즈 문제 수가 5개 미만입니다.", HttpStatus.BAD_REQUEST);
        }
        if (request.getSettings().getQuestionCount() > quiz.getQuestionCount()) {
            throw new RoomException("VALIDATION_FAILED", "설정한 문제 수가 퀴즈의 전체 문제 수를 초과합니다.", HttpStatus.BAD_REQUEST);
        }

        // 방 생성 기본 정보 세팅
        UUID roomId = UUID.randomUUID();
        String roomCode = generateUniqueRoomCode();

        // Redis에 저장할 초기 상태 객체
        RoomRedisEntity roomEntity = RoomRedisEntity.builder()
                .roomId(roomId)
                .roomCode(roomCode)
                .status(RoomRedisEntity.RoomStatus.WAITING)
                .quizId(quiz.getId())
                .quizTitle(quiz.getTitle())
                .settings(request.getSettings())
                .participants(new ArrayList<>(List.of(
                        RoomRedisEntity.Participant.builder()
                                .nickname(request.getNickname())
                                .isHost(true)
                                .isConnected(false)
                                .isReady(false)
                                .score(0)
                                .build()
                )))
                .build();

        // Redis에 저장( Room ID 기준 & Room Code 기준 양방향 매핑)
        // 방 생성 -> 아무도 없음 -> 그 상태로 아무도 안들어오면 30분 후 방 사라짐
        try {
            redisTemplate.opsForValue().set(ROOM_KEY_PREFIX + roomId, roomEntity, 30, TimeUnit.MINUTES);
            redisTemplate.opsForValue().set(CODE_KEY_PREFIX + roomCode, roomId.toString(), 30, TimeUnit.MINUTES);
        } catch (Exception e) {
            // 직렬화 실패 시 로그 출력
            e.printStackTrace();
            throw e;
        }
        // 응답 리턴
        return RoomCreateResponse.builder()
                .roomId(roomId)
                .roomCode(roomCode)
                .status(RoomRedisEntity.RoomStatus.WAITING)
                .quizTitle(quiz.getTitle())
                .settings(request.getSettings())
                .build();
    }

    // 방 코드 중복 방지
    private String generateUniqueRoomCode() {
        String code;
        int retryCount = 0;
        do {
            code = roomCodeGenerator.generate();
            retryCount++;
            // 무한루프 방지
            if (retryCount > 10) {
                throw new RoomException("ROOM_CODE_GENERATE_FAILED", "방 코드를 생성할 수 없습니다.", HttpStatus.INTERNAL_SERVER_ERROR);
            }
        } while (Boolean.TRUE.equals(redisTemplate.hasKey(CODE_KEY_PREFIX + code)));
        return code;
    }

    // 방 조회
    public RoomInfoResponse getRoomInfo(String roomCode, String clientIp) {
        // 에러케이스 - IP 기반 Rate Limiting 검사
        checkRateLimit(clientIp);

        // redis 에서 방 코드로 UUID 조회
        String roomIdStr = (String) redisTemplate.opsForValue().get(CODE_KEY_PREFIX + roomCode);
        if (roomIdStr == null) {
            throw new RoomException("ROOM_NOT_FOUND", "존재하지 않는 방 코드입니다.", HttpStatus.NOT_FOUND);
        }

        // redis 에서 실제 방 객체 조회
        RoomRedisEntity room = (RoomRedisEntity) redisTemplate.opsForValue().get(ROOM_KEY_PREFIX + roomIdStr);
        if (room == null) {
            throw new RoomException("ROOM_NOT_FOUND", "방 정보를 찾을 수 없습니다.", HttpStatus.NOT_FOUND);
        }

        // 게임 진행 상태 검사
        if (room.getStatus() != RoomRedisEntity.RoomStatus.WAITING) {
            throw new RoomException("GAME_IN_PROGRESS", "이미 게임이 진행 중이거나 종료된 방입니다.", HttpStatus.BAD_REQUEST);
        }

        // 인원수 제한 검사 (최대 8명)
        int currentPlayers = room.getParticipants() != null ? room.getParticipants().size() : 0;
        if (currentPlayers > MAX_PLAYERS) {
            throw new RoomException("ROOM_FULL", "방이 꽉 찼습니다.", HttpStatus.BAD_REQUEST);
        }

        // dto 생성 및 반환
        return RoomInfoResponse.builder()
                .roomId(room.getRoomId())
                .roomCode(room.getRoomCode())
                .status(room.getStatus())
                .currentPlayerCount(currentPlayers)
                .maxPlayerCount(MAX_PLAYERS)
                .quizTitle(room.getQuizTitle())
                .build();
    }

    // IP 기반 조회 횟수 제한
    private void checkRateLimit(String clientIp) {
        String rateKey = "rate:room_info:" + clientIp;

        // IP별 키 값 1씩 증가 (없으면 1)
        Long attempts = redisTemplate.opsForValue().increment(rateKey);

        // 처음 요청 경우 만료 시간 1분
        if (attempts != null && attempts == 1) {
            redisTemplate.expire(rateKey, 1, TimeUnit.MINUTES);
        }

        // 1분 내에 10회를 초과한 경우 에러 발생 (429 Too Many Requests)
        if (attempts != null && attempts > 10) {
            throw new RoomException("RATE_LIMITED", "방 조회 시도 횟수를 초과했습니다. 잠시 후 다시 시도해주세요.", HttpStatus.TOO_MANY_REQUESTS);
        }
    }

}
