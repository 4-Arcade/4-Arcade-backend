package com.fourarcade.arcadebackend.common.security;

import io.jsonwebtoken.*;
import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.security.Keys;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.*;

@Component
@RequiredArgsConstructor
public class JwtTokenProvider {

    // application.properties 의 jwt 설정 담아둔 프로퍼티 클래스
    private final JwtTokenProperties props;

    // 한 번만 생성해서 캐싱
    private SecretKey signingKey;

    // application.properties에 저장된 Base64 문자열 비밀키를 가져와 SecretKey 객체로 변환
    @PostConstruct
    private void init() {
        byte[] keyBytes = Decoders.BASE64.decode(props.getSecretBase64());
        this.signingKey = Keys.hmacShaKeyFor(keyBytes);
    }

    // access token 생성
    public String createAccessToken(UUID userId, String email, Collection<String> roles) {
        Instant now = Instant.now();
        Instant exp = now.plus(props.getAccessTtlMinutes(), ChronoUnit.MINUTES);

        // JWT 내부에 담을 커스텀 데이터
        Map<String, Object> claims = new HashMap<>();
        claims.put("uid", userId.toString());   // DB의 UUID를 문자열로 변환해서 저장
        claims.put("email", email);
        if (roles != null && !roles.isEmpty()) {
            claims.put("roles", roles);
        }

        return Jwts.builder()
                .header().type("JWT").and()                   // 헤더: 이 토큰은 JWT 타입이다
                .issuer(props.getIssuer())                    // 발급자 (예: arcade-backend)
                .audience().add(props.getAudience()).and()    // 대상자 (예: arcade-frontend)
                .subject(userId.toString())                   // 토큰 제목: 유저 식별자(UUID)
                .id(UUID.randomUUID().toString())             // 토큰 고유 식별자 (jti)
                .issuedAt(Date.from(now))                     // 발급 시간
                .notBefore(Date.from(now))                    // 이 시간 이전에는 사용 불가 (보통 발급시간과 동일)
                .expiration(Date.from(exp))                   // 만료 시간
                .claims(claims)                               // 위에서 만든 커스텀 데이터 쏙!
                .signWith(signingKey)                       // 비밀키로 단단하게 서명 (위조 방지)
                .compact();                                   // 압축해서 문자열로
    }

    // Access Token 검증 및 파싱
    // 토큰 확인
    public Jws<Claims> parseAndValidate(String token) throws JwtException {
        return Jwts.parser()
                .requireAudience(props.getAudience())          // 우리가 지정한 대상자가 맞는지?
                .requireIssuer(props.getIssuer())              // 우리가 발급한 게 맞는지?
                .clockSkewSeconds(props.getClockSkewSeconds()) // 서버 간 미세한 시간 오차 허용 (예: 30초)
                .verifyWith(signingKey)                      // 우리 비밀키로 서명된 거 맞아?
                .build()
                .parseSignedClaims(token);                     // 다 통과하면 내용물 꺼내기
    }

    // 단순 토큰 확인 (true/false)
    public boolean isValid(String token) {
        try {
            parseAndValidate(token);
            return true;
        } catch (JwtException | IllegalArgumentException e) {
            return false;
        }
    }

    // 유효기간 확인
    public boolean isExpired(String token) {
        try {
            parseAndValidate(token);
            return false; // 통과하면 유효
        } catch (ExpiredJwtException e) {
            return true;
        } catch (JwtException e) {
            return false;
        }
    }

    // 토큰 안에 담아뒀던 Claims 꺼내기
    public Claims getClaims(String token) throws JwtException {
        return parseAndValidate(token).getPayload();
    }

    // 토큰에서 UUID 뽑아내기
    public UUID getUserId(String token) throws JwtException {
        Claims c = getClaims(token);

        // 커스텀 클레임(uid)를 먼저 찾고, 없으면 기본 클레임(subject) 사용
        String uidStr = c.get("uid", String.class);
        if (uidStr != null) {
            return UUID.fromString(uidStr);
        }
        return UUID.fromString(c.getSubject());
    }

    // Authorization 헤더 파싱(토큰 값만 잘라냄)
    public Optional<String> resolveFromAuthorization(String authorizationHeader) {
        if (authorizationHeader == null) {
            return Optional.empty();
        }

        String prefix = "Bearer ";
        // 대소문자 무시
        if (authorizationHeader.regionMatches(true, 0, prefix, 0, prefix.length())) {
            return Optional.of(authorizationHeader.substring(prefix.length()).trim());
        }
        return Optional.empty();
    }
}
