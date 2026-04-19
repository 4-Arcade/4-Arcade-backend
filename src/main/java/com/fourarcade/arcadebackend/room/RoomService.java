package com.fourarcade.arcadebackend.room;

import com.fourarcade.arcadebackend.common.exception.RoomException;
import com.fourarcade.arcadebackend.quiz.Quiz;
import com.fourarcade.arcadebackend.quiz.QuizService;
import com.fourarcade.arcadebackend.room.util.RoomCodeGenerator;
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
                                .score(0)
                                .build()
                )))
                .build();

        // Redis에 저장( Room ID 기준 & Room Code 기준 양방향 매핑)
        // 방은 최대 2시간 동안 살아있음
        try {
            redisTemplate.opsForValue().set(ROOM_KEY_PREFIX + roomId, roomEntity, 2, TimeUnit.HOURS);
            redisTemplate.opsForValue().set(CODE_KEY_PREFIX + roomCode, roomId.toString(), 2, TimeUnit.HOURS);
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

}
