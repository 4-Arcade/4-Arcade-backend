# 4-Arcade-backend
## Music.io 🎵

> 실시간 음악 퀴즈 배틀

여러 명이 함께 방을 만들어 입장하고, 유튜브 음악을 듣고 정답을 맞히며 실시간으로 경쟁하는 멀티플레이어 퀴즈 게임의 백엔드입니다.

프론트엔드 리포지토리: https://github.com/4-Arcade/4-Arcade-frontend

---

## 📌 개요

Music.io는 친구들과 함께 유튜브 음악을 듣고 곡 정보를 맞히는 실시간 퀴즈 게임입니다. 방을 만들어 코드를 공유하면 최대 8명까지 함께 입장할 수 있고, WebSocket을 통해 모든 참가자가 실시간으로 같은 화면을 보며 게임을 진행합니다.

## ✨ 주요 특징

- **실시간 멀티플레이**: WebSocket 기반으로 방 입장부터 게임 진행, 결과 발표까지 모든 참가자에게 즉시 동기화
- **재접속 지원**: 네트워크가 끊겨도 30초 안에 재접속하면 게임에 다시 합류 가능
- **방장 자동 위임**: 방장이 나가거나 연결이 끊기면 자동으로 다음 사람에게 권한 이전
- **안전한 인증**: JWT를 Access Token과 Refresh Token으로 분리하여 발급 및 검증
- **유연한 게임 설정**: 문제 수, 제한 시간, 정답 공개 여부, 오답 허용 횟수를 방장이 직접 설정

## 🗓 개발 기간

2026.03 ~ 2026.06 (약 2.5개월)

---

## 🛠 기술 스택

| 분류 | 기술 |
|---|---|
| Language | Java 17 |
| Framework | Spring Boot, Spring Security, Spring WebSocket |
| Database | PostgreSQL (Supabase) |
| Cache / In-Memory Store | Redis (Upstash) |
| Storage | Cloudflare R2 |
| Migration | Flyway |
| Auth | JWT (Access Token + Refresh Token) |
| API 문서 | Swagger (springdoc-openapi) |
| 배포 | Render (Backend), Vercel (Frontend) |
| 프론트엔드 API | https://musicgame-orpin.vercel.app/ |

---

## 📦 주요 기능

### 인증
- 이메일/비밀번호 기반 회원가입, 로그인
- JWT Access Token + Refresh Token 발급 (Refresh Token은 해시로 DB 저장)
- Access Token 재발급
- 로그아웃

### 방(Room) 시스템
- 퀴즈 기반 방 생성 및 6자리 방 코드 발급
- Redis를 활용한 방 상태 관리 (대기/레디/게임중/결과)
- 최대 8인 동시 입장

### 실시간 게임 (WebSocket)
- 방 입장/퇴장, 레디 상태 변경, 방장 강퇴 및 위임
- 재접속 처리 (30초 유예 시간)
- 실시간 정답 제출 및 정답 판정, 스피드 보너스 점수 시스템
- 문제별 타이머 및 자동 진행
- 재생 오류 발생 시 자동 스킵
- 최종 결과 및 개인별 랭킹 전송

###  사용자 프로필 관리 (User)
- 프로필 이미지 업로드 (Cloudflare R2): S3 호환 객체 스토리지인 Cloudflare R2를 연동하여 빠르고 안정적인 유저 프로필 이미지 업로드 및 글로벌 호스팅 지원
---

## 🔑 주요 API

### 인증 (`/auth`)

| Method | Endpoint | 설명 |
|---|---|---|
| POST | `/auth/register` | 회원가입 |
| POST | `/auth/login` | 로그인 |
| POST | `/auth/refresh` | Access Token 재발급 |
| POST | `/auth/logout` | 로그아웃 |

### 방 (`/room`)

| Method | Endpoint | 설명 |
|---|---|---|
| POST | `/room` | 방 생성 |
| GET | `/room/{roomCode}` | 방 정보 조회 |

### WebSocket

| Endpoint | 설명 |
|---|---|
| `/ws/room?roomId={roomId}&nickname={nickname}` | 방 실시간 연결 |

> 더 자세한 명세는 Swagger UI(`/swagger-ui/index.html`)에서 확인할 수 있습니다.

---

## ⚙️ 실행 방법

1. 환경 변수(DB, Redis, JWT 등)를 설정합니다.
2. `./gradlew clean build` 후 `java -jar build/libs/*.jar`로 실행합니다.
3. `http://localhost:8080/swagger-ui/index.html` 에서 API 문서를 확인할 수 있습니다.

---

## 📁 패키지 구조

```
com.fourarcade.arcadebackend
├── auth          # 회원가입, 로그인, 토큰 발급/재발급
├── user           # 유저 도메인, 프로필 이미지 설정
├── room           # 방 생성, 설정, 게임 진행 로직
├── quiz           # 퀴즈 도메인
├── question       # 문제 도메인
├── mypage         # 마이페이지 (닉네임 변경, 내 퀴즈 목록 조회)
├── websocket      # 실시간 통신 핸들러
├── common
│   ├── api        # 공통 응답 포맷 (ApiResponse, ApiError)
│   ├── exception  # 커스텀 예외, 글로벌 예외 핸들러
|   ├── image      # Cloudflare R2 기반 공통 이미지 업로드
|   ├── youtube    # 유튜브 URL 파싱 및 유효성 검증
│   └── security   # JWT, Spring Security 설정
└── config         # Redis, CORS, Swagger 등 설정
```
