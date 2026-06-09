package com.fourarcade.arcadebackend.websocket;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fourarcade.arcadebackend.quiz.Quiz;
import com.fourarcade.arcadebackend.quiz.QuizService;
import com.fourarcade.arcadebackend.room.GamePlayService;
import com.fourarcade.arcadebackend.room.RoomRedisEntity;
import com.fourarcade.arcadebackend.room.RoomSettings;
import com.fourarcade.arcadebackend.websocket.dto.ErrorPayload;
import com.fourarcade.arcadebackend.websocket.dto.WsEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.scheduling.TaskScheduler;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.CloseStatus;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;
import org.springframework.web.socket.handler.TextWebSocketHandler;
import org.springframework.web.util.UriComponentsBuilder;

import java.net.URI;
import java.time.Instant;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;

@Slf4j
@Component
@RequiredArgsConstructor
public class RoomWebSocketHandler extends TextWebSocketHandler {

    private final RedisTemplate<String, Object> redisTemplate;
    private final ObjectMapper objectMapper = new ObjectMapper();    // 기본 ObjectMapper 주입받음
    private final QuizService quizService;      // 퀴즈 문항 수 검증
    private final GamePlayService gamePlayService;
    private final TaskScheduler taskScheduler;

    private static final String ROOM_KEY_PREFIX = "room:";
    private static final String CODE_KEY_PREFIX = "room:code:";

    // 메모리 세션 관리
    // 방마다 어떤 세션들이 있는지 저장: roomId -> Set<Session>
    private final Map<String, Set<WebSocketSession>> roomSessions = new ConcurrentHashMap<>();
    // 이 세션이 어떤 방의 누구인지 추적: sessionId -> roomId
    private final Map<String, String> sessionToRoomMap = new ConcurrentHashMap<>();
    // 이 세션의 닉네임이 무엇인지 추적: sessionId -> nickname
    private final Map<String, String> sessionToNicknameMap = new ConcurrentHashMap<>();

    @Override
    @SuppressWarnings("resource")
    public void afterConnectionEstablished(WebSocketSession session) throws Exception {
        URI uri = session.getUri();
        if (uri == null) {
            session.close();
            return;
        }

        // URI 에서 파라미터 추출 <?roomId=...&nickname=...)
        Map<String, String> queryParams = UriComponentsBuilder.fromUri(uri)
                .build().getQueryParams().toSingleValueMap();

        String roomId = queryParams.get("roomId");
        String nickname = queryParams.get("nickname");  // URL 인코딩은 String 이 자동 디코딩함

        // 방 존재 여부 확인
        RoomRedisEntity room = (RoomRedisEntity) redisTemplate.opsForValue().get(ROOM_KEY_PREFIX + roomId);
        if (room == null) {
            sendErrorAndClose(session, "ROOM_NOT_FOUND");
            return;
        }

        // 닉네임 중복 및 재접속 여부 선행 확인
        RoomRedisEntity.Participant existingPlayer = room.getParticipants().stream()
                .filter(p -> p.getNickname().equals(nickname))
                .findFirst()
                .orElse(null);

        boolean isReconnection = false;

        if (existingPlayer != null) {
            if (existingPlayer.getIsConnected()) {
                // 유령 세션 판별
                boolean isGhostSession = (findSessionByNickname(roomId, nickname) == null);

                if (isGhostSession) {
                    log.info("유령 세션 발견. 닉네임: {} -> 강제 재접속으로 처리.", nickname);
                    isReconnection = true;
                    existingPlayer.setIsConnected(true);
                    existingPlayer.setDisconnectedAt(null);
                } else {
                    // 닉네임 중복
                    sendErrorAndClose(session, "NICKNAME_TAKEN");
                    return;
                }
            } else {
                long now = System.currentTimeMillis();
                long disconnectedAt =
                        existingPlayer.getDisconnectedAt() != null ? existingPlayer.getDisconnectedAt() : 0;
                boolean isWithin30Seconds = (now - disconnectedAt) <= 30000;
                boolean isResultState = room.getStatus() == RoomRedisEntity.RoomStatus.RESULT;

                if (isWithin30Seconds || isResultState) {
                    // 재접속 조건 충족 (3, 4번 건너뜀)
                    isReconnection = true;
                    existingPlayer.setIsConnected(true);
                    existingPlayer.setDisconnectedAt(null);
                } else {
                    // 30초 초과 & RESULT 아님 -> 기존 정보 삭제 후 신규 입장으로 취급
                    room.getParticipants().remove(existingPlayer);
                }
            }
        }

        // 신규 입장 검증 (재접속 X)
        if (!isReconnection) {
            if (room.getStatus() == RoomRedisEntity.RoomStatus.IN_GAME ||
                room.getStatus() == RoomRedisEntity.RoomStatus.RESULT) {
                sendErrorAndClose(session, "GAME_IN_PROGRESS");
                return;
            }
            if (room.getParticipants().size() >= 8) {
                sendErrorAndClose(session, "ROOM_FULL");
                return;
            }

            // 신규 플레이어 객체 생성 및 추가
            boolean hasHost = room.getParticipants().stream()
                    .anyMatch(RoomRedisEntity.Participant::getIsHost);

            RoomRedisEntity.Participant newPlayer = RoomRedisEntity.Participant.builder()
                    .nickname(nickname)
                    .isHost(!hasHost)   // 방장이 없을 때만 내가 방장이 됨
                    .isReady(false)
                    .isConnected(true)
                    .score(0)
                    .build();
            room.getParticipants().add(newPlayer);

            // 방이 READY 상태였는데 새 유저(isReady=false)가 들어왔으므로 WAITING 으로 변경
            if (room.getStatus() == RoomRedisEntity.RoomStatus.READY) {
                room.setStatus(RoomRedisEntity.RoomStatus.WAITING);
            }
        }

        // Redis 업데이트 및 이벤트 전송
        redisTemplate.opsForValue().set(ROOM_KEY_PREFIX + roomId, room);

        // 누군가 접속하면 TTL 제거
        redisTemplate.persist(ROOM_KEY_PREFIX + roomId);
        redisTemplate.persist(CODE_KEY_PREFIX + room.getRoomCode());

        // 세션 메모리 등록
        roomSessions.computeIfAbsent(roomId, k -> Collections.synchronizedSet(new HashSet<>())).add(session);
        sessionToRoomMap.put(session.getId(), roomId);
        sessionToNicknameMap.put(session.getId(), nickname);

        // S -> C (본인): room:state 전송
        sendRoomState(session, room);

        // Broadcast 처리
        if (isReconnection) {
            // [재접속] player:reconnected 전송
            WsEvent<Map<String, Object>> reconnectedEvent = WsEvent.<Map<String, Object>>builder()
                    .event("player:reconnected")
                    .data(Map.of("nickname", nickname))
                    .build();
            broadcastToRoom(roomId, reconnectedEvent, session);
            log.info("Player {} reconnected to room {}", nickname, roomId);

            // RESULT 상태면 game:result 개인화 전송
            if (room.getStatus() == RoomRedisEntity.RoomStatus.RESULT) {
                gamePlayService.sendGameResultToPlayer(roomId, nickname);
            }
        } else {
            // [신규 입장] player:joined 전송 (playerCount 포함)
            WsEvent<Map<String, Object>> joinedEvent = WsEvent.<Map<String, Object>>builder()
                    .event("player:joined")
                    .data(Map.of(
                            "nickname", nickname,
                            "playerCount", room.getParticipants().size()
                    ))
                    .build();
            broadcastToRoom(roomId, joinedEvent, session);
            log.info("New Player {} joined room {}", nickname, roomId);
        }
        broadcastPlayersUpdated(roomId, room);
    }

    // 연결 종료
    @Override
    public void afterConnectionClosed(WebSocketSession session, CloseStatus status) {
        String sessionId = session.getId();
        String roomId = sessionToRoomMap.remove(sessionId);
        String nickname = sessionToNicknameMap.remove(sessionId);

        // 비정상적 종료
        if (roomId == null || nickname == null) return;

        // 메모리 세션 리스트에서 제거
        Set<WebSocketSession> sessions = roomSessions.get(roomId);
        if (sessions != null) {
            sessions.remove(session);
            if (sessions.isEmpty()) {
                roomSessions.remove(roomId);    // 방에 아무도 없으면 메모리 누수 방지
            }
        }

        // redis update
        RoomRedisEntity room = (RoomRedisEntity) redisTemplate.opsForValue().get(ROOM_KEY_PREFIX + roomId);
        if (room != null) {
            // 튕긴 유저 찾기
            RoomRedisEntity.Participant disconnectedPlayer = room.getParticipants().stream()
                    .filter(p -> p.getNickname().equals(nickname))
                    .findFirst()
                    .orElse(null);

            if (disconnectedPlayer != null) {
                // 상태 업데이트
                disconnectedPlayer.setIsConnected(false);
                disconnectedPlayer.setDisconnectedAt(System.currentTimeMillis());

                // host 가 튕겼을 때 자동 host 이관
                if (disconnectedPlayer.getIsHost()) {
                    // 접속 중인 사람 중 가장 먼저 들어온 사람 찾기
                    RoomRedisEntity.Participant newHost = room.getParticipants().stream()
                            .filter(RoomRedisEntity.Participant::getIsConnected)
                            .findFirst()
                            .orElse(null);

                    // new Host 선정
                    if (newHost != null) {
                        disconnectedPlayer.setIsHost(false);

                        newHost.setIsHost(true);
                        newHost.setIsReady(false);  // 새 방장은 레디 해제

                        // IN_GAME 이나 RESULT 상태가 아닐 때만 레디 해제 및 웨이팅 강등
                        if (room.getStatus() != RoomRedisEntity.RoomStatus.IN_GAME &&
                            room.getStatus() != RoomRedisEntity.RoomStatus.RESULT) {
                            room.setStatus(RoomRedisEntity.RoomStatus.WAITING);
                            room.getParticipants().forEach(p -> p.setIsReady(false));
                        }

                        // 방 전체에 host:changed 이벤트 전송
                        WsEvent<Map<String, String>> hostChangedEvent = WsEvent.<Map<String, String>>builder()
                                .event("host:changed")
                                .data(Map.of("newHostNickname", newHost.getNickname()))
                                .build();
                        broadcastToRoom(roomId, hostChangedEvent, null);

                        log.info("방장 {} 튕김 {}에게 방장 권한 자동 이관", nickname, newHost.getNickname());
                    }
                }
            }

            // redis 저장
            redisTemplate.opsForValue().set(ROOM_KEY_PREFIX + roomId, room);

            boolean anyoneConnected = room.getParticipants().stream()
                    .anyMatch(RoomRedisEntity.Participant::getIsConnected);

            if (!anyoneConnected) {
                // 아무도 없으면 30분 후 자동 삭제
                redisTemplate.opsForValue().set(ROOM_KEY_PREFIX + roomId, room, 30, TimeUnit.MINUTES);
                redisTemplate.opsForValue().set(CODE_KEY_PREFIX + room.getRoomCode(),
                        roomId, 30, TimeUnit.MINUTES);
                log.info("Room {} is empty. Will be deleted in 30 minutes.", roomId);
            }

            // 남은 사람들에게 브로드캐스트
            // 방 해산 X -> 남은 사람들에게 '재접속 대기' 상태임 알림
            WsEvent<Map<String, Object>> disconnectEvent = WsEvent.<Map<String, Object>>builder()
                    .event("player:disconnected")
                    .data(Map.of(
                            "nickname", nickname,
                            "reconnectTimeoutSec", 30
                    ))
                    .build();
            broadcastToRoom(roomId, disconnectEvent, null);
            log.info("Player {} disconnected from room {}. Waiting for reconnection...", nickname, roomId);
            broadcastPlayersUpdated(roomId, room);

            // result 상태가 아닐 때만 30초 뒤 강퇴 검사 타이머 실행
            if (room.getStatus() != RoomRedisEntity.RoomStatus.RESULT) {
                taskScheduler.schedule(() -> checkAndKickTimeoutPlayer(roomId, nickname),
                        Instant.now().plusSeconds(30));
            }
        }
    }

    // 클라이언트 메시지 수신 (C -> S) 라우터
    @Override
    protected void handleTextMessage(WebSocketSession session, TextMessage message) throws Exception {
        String sessionId = session.getId();
        String roomId = sessionToRoomMap.get(sessionId);
        String nickname = sessionToNicknameMap.get(sessionId);

        if (roomId == null || nickname == null) return;

        // JSON 파싱
        JsonNode rootNode = objectMapper.readTree(message.getPayload());
        String event = rootNode.path("event").asText();
        JsonNode data = rootNode.path("data");

        // Redis 에서 최신 방 상태 불러오기
        RoomRedisEntity room = (RoomRedisEntity) redisTemplate.opsForValue().get(ROOM_KEY_PREFIX + roomId);
        if (room == null) return;

        // 이벤트 각 핸들러로 라우팅
        switch (event) {
            case "player:ready":
                handlePlayerReady(room, nickname, data);
                break;
            case "host:kick":
                handleHostKick(session, room, nickname, data);
                break;
            case "host:settings_update":
                handleSettingsUpdate(session, room, nickname, data);
                break;
            case "host:disband":
                handleHostDisband(session, room, nickname);
                break;
            case "host:change":
                handleHostChange(session, room, nickname, data);
                break;
            case "player:left":
                handlePlayerLeft(session, room, nickname);
                break;
            case "game:start":
                handleGameStart(session, roomId, nickname);
                break;
            case "game:answer":
                handleGameAnswer(roomId, nickname, data);
                break;
            case "question:playback_error":
                handlePlaybackError(room, data);
                break;
            case "game:restart":
                handleGameRestart(session, roomId, nickname);
                break;
            default:
                log.warn("알 수 없는 이벤트 수신: {}", event);
        }
    }

    // [C -> S] player:ready (레디 상태 변경)
    private void handlePlayerReady(RoomRedisEntity room, String nickname, JsonNode data) {
        boolean isReady = data.path("isReady").asBoolean();

        RoomRedisEntity.Participant player = getParticipant(room, nickname);
        if (player == null || player.getIsHost()) return;   // 방장은 레디 X

        player.setIsReady(isReady);

        // 방 상태 전이 로직 (WAITING <-> READY)
        // 방장 제외 '접속 중'인 모든 플레이어 레디 상태인지 확인
        boolean allReady = room.getParticipants().stream()
                .filter(p -> !p.getIsHost() && p.getIsConnected())
                .allMatch(RoomRedisEntity.Participant::getIsReady);

        // 혼자 있을 때는 ready 불가, 2명 이상일 때만 가능
        long connectedCount = room.getParticipants().stream().filter(RoomRedisEntity.Participant::getIsConnected).count();

        if (allReady && connectedCount > 1) {
            room.setStatus(RoomRedisEntity.RoomStatus.READY);
        } else {
            room.setStatus(RoomRedisEntity.RoomStatus.WAITING);
        }

        redisTemplate.opsForValue().set(ROOM_KEY_PREFIX + room.getRoomId(), room);

        // room:players_updated broadcast
        broadcastPlayersUpdated(room.getRoomId().toString(), room);
    }

    // [C -> S] host:kick (강퇴)
    private void handleHostKick(WebSocketSession session, RoomRedisEntity room, String hostNickname, JsonNode data) throws Exception {
        RoomRedisEntity.Participant host = getParticipant(room, hostNickname);
        if (host == null || !host.getIsHost()) {
            sendError(session, "UNAUTHORIZED");
            return;
        }  // 방장만 가능

        String targetNickname = data.path("targetNickname").asText();
        RoomRedisEntity.Participant target = getParticipant(room, targetNickname);

        if (target != null && !target.getIsHost()) {
            // 타겟 유저 세션 찾기 및 강제 종료
            WebSocketSession targetSession = findSessionByNickname(room.getRoomId().toString(), targetNickname);
            if (targetSession != null) {
                WsEvent<Map<String, String>> kickedEvent = WsEvent.<Map<String, String>>builder()
                        .event("player:kicked")
                        .data(Map.of("message", "방에서 강퇴되었습니다"))
                        .build();
                targetSession.sendMessage(new TextMessage(objectMapper.writeValueAsString(kickedEvent)));
                targetSession.close(CloseStatus.NOT_ACCEPTABLE);
            }

            // Redis 유저 삭제
            room.getParticipants().remove(target);
            redisTemplate.opsForValue().set(ROOM_KEY_PREFIX + room.getRoomId(), room);

            // 남은 사람들에게 player:left 및 players_updated 브로드캐스트
            WsEvent<Map<String, Object>> leftEvent = WsEvent.<Map<String, Object>>builder()
                    .event("player:left")
                    .data(Map.of("nickname", targetNickname, "playerCount", room.getParticipants().size()))
                    .build();
            broadcastToRoom(room.getRoomId().toString(), leftEvent, null);
            broadcastPlayersUpdated(room.getRoomId().toString(), room);
        }
    }

    // [C -> S] host:settings_update (설정 변경)
    private void handleSettingsUpdate(WebSocketSession session, RoomRedisEntity room, String hostNickname, JsonNode data) throws Exception {
        RoomRedisEntity.Participant host = getParticipant(room, hostNickname);
        if (host == null || !host.getIsHost()) return;

        if (room.getStatus() != RoomRedisEntity.RoomStatus.WAITING && room.getStatus() != RoomRedisEntity.RoomStatus.READY) {
            return; // 대기 또는 레디 상태에서만 변경 가능
        }

        JsonNode settingsNode = data.path("settings");
        int questionCount = settingsNode.path("questionCount").asInt();
        int timeLimit = settingsNode.path("timeLimit").asInt();

        // 유효성 검사 (questionCount 5~20, timeLimit 10~30)
        if (questionCount < 5 || questionCount > 20 || timeLimit < 10 || timeLimit > 30) {
            sendError(session, "INVALID_SETTINGS");
            return;
        }

        // DB 최신 문제 수 조회 및 초과 여부 검사
        Quiz quiz = quizService.getQuizById(room.getQuizId());
        if (questionCount > quiz.getQuestionCount()) {
            sendError(session, "EXCEEDS_MAX_QUESTIONS");
            return;
        }

        // Redis 설정 업데이트
        RoomSettings newSettings = objectMapper.treeToValue(settingsNode, RoomSettings.class);
        room.setSettings(newSettings);

        // 상태 rollback (ready)
        if (room.getStatus() == RoomRedisEntity.RoomStatus.READY) {
            room.setStatus(RoomRedisEntity.RoomStatus.WAITING);
            room.getParticipants().forEach(p -> {
                if (!p.getIsHost()) p.setIsReady(false); // 호스트 제외 모두 레디 해제
            });
            // 변경된 상태로 방 전체에 알림
            broadcastPlayersUpdated(room.getRoomId().toString(), room);
        }

        redisTemplate.opsForValue().set(ROOM_KEY_PREFIX + room.getRoomId(), room);

        // room:setting_updated 방 전체 전송
        WsEvent<Map<String, Object>> settingsUpdatedEvent = WsEvent.<Map<String, Object>>builder()
                .event("room:settings_updated")
                .data(Map.of("settings", newSettings))
                .build();
        broadcastToRoom(room.getRoomId().toString(), settingsUpdatedEvent, null);
    }

    // [C -> S] host:disband (방 해산)
    private void handleHostDisband(WebSocketSession session, RoomRedisEntity room, String hostNickname) throws Exception {
        RoomRedisEntity.Participant host = getParticipant(room, hostNickname);
        if (host == null || !host.getIsHost()) {
            sendError(session, "UNAUTHORIZED");
            return;
        }

        // 방 전체에 room:disbanded 전송
        WsEvent<Map<String, Object>> disbandEvent = WsEvent.<Map<String, Object>>builder()
                .event("room:disbanded")
                .data(Map.of("message", "방장이 방을 해산했습니다."))
                .build();
        broadcastToRoom(room.getRoomId().toString(), disbandEvent, null);

        // redis key 삭제
        redisTemplate.delete(ROOM_KEY_PREFIX + room.getRoomId());
        redisTemplate.delete(CODE_KEY_PREFIX + room.getRoomCode());

        // 모든 세션 종료 및 메모리 정리
        Set<WebSocketSession> sessions = roomSessions.remove(room.getRoomId().toString());
        if (sessions != null) {
            for (WebSocketSession s : sessions) {
                if (s.isOpen()) s.close(CloseStatus.NORMAL);
                sessionToRoomMap.remove(s.getId());
                sessionToNicknameMap.remove(s.getId());
            }
        }
    }

    // 호스트 변경
    private void handleHostChange(WebSocketSession session, RoomRedisEntity room, String currentHostNickname, JsonNode data) throws Exception {
        RoomRedisEntity.Participant currentHost = getParticipant(room, currentHostNickname);

        if (currentHost == null || !currentHost.getIsHost()) {
            sendError(session, "UNAUTHORIZED");
            return;
        }

        String targetNickname = data.path("targetNickname").asText();
        RoomRedisEntity.Participant newHost = getParticipant(room, targetNickname);

        if (newHost != null && !newHost.getIsHost()) {
            currentHost.setIsHost(false);
            newHost.setIsHost(true);

            room.setStatus(RoomRedisEntity.RoomStatus.WAITING);

            room.getParticipants().forEach(p -> p.setIsReady(false));

            redisTemplate.opsForValue().set(ROOM_KEY_PREFIX + room.getRoomId(), room);

            WsEvent<Map<String, String>> hostChangedEvent = WsEvent.<Map<String, String>>builder()
                    .event("host:changed")
                    .data(Map.of("newHostNickname", targetNickname))
                    .build();
            broadcastToRoom(room.getRoomId().toString(), hostChangedEvent, null);
            broadcastPlayersUpdated(room.getRoomId().toString(), room);
        }
    }

    // 방 퇴장
    private void handlePlayerLeft(WebSocketSession session, RoomRedisEntity room, String nickname) throws Exception {
        RoomRedisEntity.Participant leftPlayer = getParticipant(room, nickname);
        if (leftPlayer == null) return;

        // 나가는 시점에 방이 게임 중이었는지 기억
        boolean wasInGame = (room.getStatus() == RoomRedisEntity.RoomStatus.IN_GAME);

        room.getParticipants().remove(leftPlayer);

        // 방장 이관
        if (leftPlayer.getIsHost() && !room.getParticipants().isEmpty()) {
            RoomRedisEntity.Participant newHost = room.getParticipants().get(0);
            newHost.setIsHost(true);
            newHost.setIsReady(false);   // 새 방장은 레디 해제

            WsEvent<Map<String, String>> hostChangedEvent = WsEvent.<Map<String, String>>builder()
                    .event("host:changed")
                    .data(Map.of("newHostNickname", newHost.getNickname()))
                    .build();
            broadcastToRoom(room.getRoomId().toString(), hostChangedEvent, session);
        }

        if (room.getParticipants().isEmpty()) {
            // 빈 명단 상태를 Redis 에 확실하게 덮어써서 좀비 부활 방지, 모두 나갔으면 30분 뒤 삭제
            redisTemplate.opsForValue().set(ROOM_KEY_PREFIX + room.getRoomId(), room, 30, TimeUnit.MINUTES);
            redisTemplate.opsForValue().set(CODE_KEY_PREFIX + room.getRoomCode(),
                    room.getRoomId().toString(), 30, TimeUnit.MINUTES);
            log.info("Room {} is empty. Will be deleted in 30 minutes.", room.getRoomId());
        } else {
            // 방 상태 대기실로 초기화
            room.setStatus(RoomRedisEntity.RoomStatus.WAITING);
            room.getParticipants().forEach(p -> p.setIsReady(false));

            // 게임 중에 나갔는데 남은 인원이 1명일 시
            if (wasInGame) {
                long connectedCount = room.getParticipants().stream()
                        .filter(RoomRedisEntity.Participant::getIsConnected).count();
                if (connectedCount < 2) {
                    log.info("방 {}: 누군가 퇴장하여 인원 부족으로 게임 강제 종료", room.getRoomId());

                    room.setStatus(RoomRedisEntity.RoomStatus.WAITING);
                    room.getParticipants().forEach(p -> p.setIsReady(false));
                    room.setGameProgress(null);

                    redisTemplate.opsForValue().set(ROOM_KEY_PREFIX + room.getRoomId(), room);

                    // 남은 1명에게 에러 띄우기
                    WsEvent<ErrorPayload> errorEvent = WsEvent.<ErrorPayload>builder()
                            .event("error")
                            .data(new ErrorPayload("NOT_ENOUGH_PLAYERS"))
                            .build();
                    broadcastToRoom(room.getRoomId().toString(), errorEvent, session);
                    broadcastRoomState(room.getRoomId().toString(), room);
                    broadcastPlayersUpdated(room.getRoomId().toString(), room);

                    // return 또는 아래 일반 set 이 실행되지 않게 처리
                    session.close(CloseStatus.NORMAL);
                    return;
                }
            }

            room.setStatus(RoomRedisEntity.RoomStatus.WAITING);
            room.getParticipants().forEach(p -> p.setIsReady(false));
            redisTemplate.opsForValue().set(ROOM_KEY_PREFIX + room.getRoomId(), room);

            WsEvent<Map<String, Object>> leftEvent = WsEvent.<Map<String, Object>>builder()
                    .event("player:left")
                    .data(Map.of(
                            "nickname", nickname,
                            "playerCount", room.getParticipants().size()
                    ))
                    .build();
            broadcastToRoom(room.getRoomId().toString(), leftEvent, session);
            broadcastPlayersUpdated(room.getRoomId().toString(), room);
        }
        session.close(CloseStatus.NORMAL);  // 정상 종료
    }

    // game:start 핸들러
    private void handleGameStart(WebSocketSession session, String roomId, String nickname) throws Exception {
        RoomRedisEntity room = (RoomRedisEntity) redisTemplate.opsForValue().get(ROOM_KEY_PREFIX + roomId);
        if (room == null) return;

        RoomRedisEntity.Participant player = getParticipant(room, nickname);
        if (player == null || !player.getIsHost()) {
            sendError(session, "UNAUTHORIZED");
            return;
        }
        gamePlayService.startGame(roomId, nickname);
    }

    // game:answer 핸들러
    private void handleGameAnswer(String roomId, String nickname, JsonNode data) {
        String answer = data.path("answer").asText(null);
        if (answer != null) {
            gamePlayService.processAnswer(roomId, nickname, answer);
        }
    }

    // game:restart 핸들러
    private void handleGameRestart(WebSocketSession session, String roomId, String nickname) throws Exception {
        RoomRedisEntity room = (RoomRedisEntity) redisTemplate.opsForValue().get(ROOM_KEY_PREFIX + roomId);
        if (room == null) return;

        RoomRedisEntity.Participant player = getParticipant(room, nickname);

        if (player == null || !player.getIsHost()) {
            sendError(session, "UNAUTHORIZED");
            return;
        }
        if (room.getStatus() != RoomRedisEntity.RoomStatus.RESULT) {
            sendError(session,"INVALID_STATE");
            return;
        }

        // WAITING 으로 초기화
        room.setStatus(RoomRedisEntity.RoomStatus.WAITING);
        room.getParticipants().forEach(p -> p.setIsReady(false));
        room.setGameProgress(null); // 진행 데이터 날림
        redisTemplate.opsForValue().set(ROOM_KEY_PREFIX + room.getRoomId(), room);

        // 방 전체에 초기화된 상태 브로드캐스트
        broadcastRoomState(roomId, room);
        broadcastPlayersUpdated(roomId, room);
    }

    // 재생 오류 감지 시 서버에 보고 (question:playback_error)
    private void handlePlaybackError(RoomRedisEntity room, JsonNode data) {
        int questionIndex = data.path("questionIndex").asInt();
        int errorCode = data.path("errorCode").asInt();

        // 서비스 호출
        gamePlayService.handlePlaybackError(room.getRoomId().toString(), questionIndex, errorCode);
    }

    // ------------------- Helper Methods ------------------- //

    // 공통 브로드캐스트 헬퍼: room:players_updated
    private void broadcastPlayersUpdated(String roomId, RoomRedisEntity room) {
        WsEvent<Map<String, Object>> updateEvent = WsEvent.<Map<String, Object>>builder()
                .event("room:players_updated")
                .data(Map.of(
                        "status", room.getStatus(),
                        "players", room.getParticipants()
                ))
                .build();
        broadcastToRoom(roomId, updateEvent, null);
    }

    private RoomRedisEntity.Participant getParticipant(RoomRedisEntity room, String nickname) {
        return room.getParticipants().stream()
                .filter(p -> p.getNickname().equals(nickname))
                .findFirst().orElse(null);
    }

    private WebSocketSession findSessionByNickname(String roomId, String nickname) {
        Set<WebSocketSession> sessions = roomSessions.get(roomId);
        if (sessions == null) return null;
        for (WebSocketSession s : sessions) {
            if (nickname.equals(sessionToNicknameMap.get(s.getId()))) {
                return s;
            }
        }
        return null;
    }

    // 핸들러 내부에서 세션이 있을 때 빠르게 에러 쏘는 용도
    private void sendError(WebSocketSession session, String errorCode) throws Exception {
        WsEvent<ErrorPayload> event = WsEvent.<ErrorPayload>builder()
                .event("error")
                .data(new ErrorPayload(errorCode))
                .build();
        session.sendMessage(new TextMessage(objectMapper.writeValueAsString(event)));
    }

    // GamePlayService 등 외부에서 방 번호와 닉네임으로 에러 쏘는 용도
    public void sendError(String roomId, String nickname, String errorCode, String errorMessage) {
        Map<String, String> errorData = Map.of(
                "errorCode", errorCode,
                "message", errorMessage
        );

        WsEvent<Map<String, String>> event = WsEvent.<Map<String, String>>builder()
                .event("error")
                .data(errorData)
                .build();

        sendToPlayer(roomId, nickname, event);  // 알아서 세션 찾아서 보내주는 헬퍼
    }

    // 특정 방에 있는 모두에게 메시지
    public void broadcastToRoom(String roomId, WsEvent<?> event, WebSocketSession excludeSession) {
        Set<WebSocketSession> sessions = roomSessions.get(roomId);
        if (sessions == null || sessions.isEmpty()) return;

        try {
            String message = objectMapper.writeValueAsString(event);
            TextMessage textMessage = new TextMessage(message);

            for (WebSocketSession s : sessions) {
                // 열려있고, 제외할 세션이 아니면 전송
                if (s.isOpen() && !s.equals(excludeSession)) {
                    s.sendMessage(textMessage);
                }
            }
        } catch (Exception e) {
            log.error("Failed to broadcast message to room {}: {}", roomId, e.getMessage());
        }
    }

    // 방 전체에 현재 상태 (room:state) 브로드캐스트
    private void broadcastRoomState(String roomId, RoomRedisEntity room) {
        String hostNickname = room.getParticipants().stream()
                .filter(RoomRedisEntity.Participant::getIsHost)
                .map(RoomRedisEntity.Participant::getNickname)
                .findFirst().orElse("Unknown");

        Map<String, Object> stateData = new HashMap<>();
        stateData.put("roomId", room.getRoomId());
        stateData.put("roomCode", room.getRoomCode());
        stateData.put("status", room.getStatus());
        stateData.put("hostNickname", hostNickname);
        stateData.put("players", room.getParticipants());
        stateData.put("settings", room.getSettings());
        if (room.getStatus() == RoomRedisEntity.RoomStatus.IN_GAME) {
            stateData.put("gameProgress", room.getGameProgress());
        }

        WsEvent<Map<String, Object>> event = WsEvent.<Map<String, Object>>builder()
                .event("room:state")
                .data(stateData)
                .build();

        broadcastToRoom(roomId, event, null);
    }

    private void sendErrorAndClose(WebSocketSession session, String errorCode) throws Exception {
        WsEvent<ErrorPayload> event = WsEvent.<ErrorPayload>builder()
                .event("error")
                .data(new ErrorPayload(errorCode))
                .build();
        session.sendMessage(new TextMessage(objectMapper.writeValueAsString(event)));
        session.close(CloseStatus.NOT_ACCEPTABLE);
    }

    private void sendRoomState(WebSocketSession session, RoomRedisEntity room) throws Exception {
        String hostNickname = room.getParticipants().stream()
                .filter(RoomRedisEntity.Participant::getIsHost)
                .map(RoomRedisEntity.Participant::getNickname)
                .findFirst().orElse("Unknown");

        // 클라이언트 전송용 DTO
        Map<String, Object> stateData = new HashMap<>();
        stateData.put("roomId", room.getRoomId());
        stateData.put("roomCode", room.getRoomCode());
        stateData.put("status", room.getStatus());
        stateData.put("hostNickname", hostNickname);
        stateData.put("players", room.getParticipants());
        stateData.put("settings", room.getSettings());
        if (room.getStatus() == RoomRedisEntity.RoomStatus.IN_GAME) {
            stateData.put("gameProgress", room.getGameProgress());
        }

        WsEvent<Map<String, Object>> event = WsEvent.<Map<String, Object>>builder()
                .event("room:state")
                .data(stateData)
                .build();

        session.sendMessage(new TextMessage(objectMapper.writeValueAsString(event)));
    }

    // 특정 유저에게만 메시지 전송 (Unicast)
    public void sendToPlayer(String roomId, String nickname, WsEvent<?> event) {
        String sessionId = sessionToNicknameMap.entrySet().stream()
                .filter(e -> e.getValue().equals(nickname) && sessionToRoomMap.get(e.getKey()).equals(roomId))
                .map(Map.Entry::getKey)
                .findFirst()
                .orElse(null);

        if (sessionId != null) {
            Set<WebSocketSession> sessions = roomSessions.get(roomId);
            if (sessions != null) {
                sessions.stream()
                        .filter(s -> s.getId().equals(sessionId) && s.isOpen())
                        .findFirst()
                        .ifPresent(s -> {
                            try {
                                s.sendMessage(new TextMessage(objectMapper.writeValueAsString(event)));
                            } catch (Exception ex) {
                                log.error("개별 메시지 전송 실패: {}", ex.getMessage());
                            }
                        });
            }
        }
    }

    // 30초 타임아웃 처리 로직
    private void checkAndKickTimeoutPlayer(String roomId, String nickname) {
        RoomRedisEntity room = (RoomRedisEntity) redisTemplate.opsForValue().get(ROOM_KEY_PREFIX + roomId);
        if (room == null || room.getStatus() == RoomRedisEntity.RoomStatus.RESULT) return;

        RoomRedisEntity.Participant targetPlayer = getParticipant(room, nickname);

        // 30초가 지났는데도 여전히 연결이 안 되어 있다면 강제 퇴장 처리
        if (targetPlayer != null && !targetPlayer.getIsConnected()) {
            // player 배열에서 해당 플레이어 제거
            room.getParticipants().remove(targetPlayer);

            // 호스트 자동 이관
            if (targetPlayer.getIsHost() && !room.getParticipants().isEmpty()) {
                RoomRedisEntity.Participant newHost = room.getParticipants().get(0);
                newHost.setIsHost(true);

                if (room.getStatus() == RoomRedisEntity.RoomStatus.WAITING ||
                        room.getStatus() == RoomRedisEntity.RoomStatus.READY) {
                    room.setStatus(RoomRedisEntity.RoomStatus.WAITING);
                    room.getParticipants().forEach(p -> p.setIsReady(false));
                }

                WsEvent<Map<String, String>> hostChangedEvent = WsEvent.<Map<String, String>>builder()
                        .event("host:changed")
                        .data(Map.of("newHostNickname", newHost.getNickname()))
                        .build();
                broadcastToRoom(roomId, hostChangedEvent, null);
            }

            if (room.getParticipants().isEmpty()) {
                redisTemplate.opsForValue().set(ROOM_KEY_PREFIX + roomId, room, 30, TimeUnit.MINUTES);
                redisTemplate.expire(CODE_KEY_PREFIX + room.getRoomCode(), 30, TimeUnit.MINUTES);
                return;
            }

            redisTemplate.opsForValue().set(ROOM_KEY_PREFIX + roomId, room);

            // 방 전체에 player:left 이벤트 전송
            WsEvent<Map<String, Object>> leftEvent = WsEvent.<Map<String, Object>>builder()
                    .event("player:left")
                    .data(Map.of(
                            "nickname", nickname,
                            "playerCount", room.getParticipants().size()
                    ))
                    .build();
            broadcastToRoom(roomId, leftEvent, null);
            broadcastRoomState(roomId, room);

            log.info("유저 {} 타임아웃으로 방 {} 에서 자동 퇴장 처리됨.", nickname, roomId);

            // IN_GAME 중 접속 참여자가 1명 이하로 감소하면 즉시 방 폭파
            if (room.getStatus() == RoomRedisEntity.RoomStatus.IN_GAME) {
                long connectedCount = room.getParticipants().stream().filter(RoomRedisEntity.Participant::getIsConnected).count();
                if (connectedCount < 2) {
                    log.info("방 {}: 인원 부족으로 게임 강제 종료", roomId);

                    // 남은 1명에게 에러 띄우고 방 대기실(WAITING)로 강제 복귀
                    WsEvent<ErrorPayload> errorEvent = WsEvent.<ErrorPayload>builder()
                            .event("error")
                            .data(new ErrorPayload("NOT_ENOUGH_PLAYERS"))
                            .build();
                    broadcastToRoom(roomId, errorEvent, null);

                    room.setStatus(RoomRedisEntity.RoomStatus.WAITING);
                    room.getParticipants().forEach(p -> p.setIsReady(false));
                    room.setGameProgress(null);
                    redisTemplate.opsForValue().set(ROOM_KEY_PREFIX + roomId, room);
                    broadcastRoomState(roomId, room);
                }
            }
        }
    }

}
