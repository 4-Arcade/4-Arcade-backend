package com.fourarcade.arcadebackend.auth;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "refresh_tokens")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class RefreshToken {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private long id;

    @Column(nullable = false)
    private UUID userId;

    @Column(name = "token_hash", nullable = false)
    private byte[] tokenHash;   // 원문 대신 해시 저장

    @Column(nullable = false)
    private LocalDateTime expiresAt;

    private LocalDateTime revokedAt; // 로그아웃 시 사용 (지금은 null 유지)

    @Builder
    public RefreshToken(UUID userId, byte[] tokenHash, LocalDateTime expiresAt) {
        this.userId = userId;
        this.tokenHash = tokenHash;
        this.expiresAt = expiresAt;
    }
}
