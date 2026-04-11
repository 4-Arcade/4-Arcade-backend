CREATE TABLE refresh_tokens (
                                id BIGSERIAL PRIMARY KEY,           -- GenerationType.IDENTITY에 해당
                                user_id UUID NOT NULL,              -- 유저 PK (UUID 타입)
                                token_hash BYTEA NOT NULL,          -- byte[] 배열은 PostgreSQL에서 BYTEA로 저장
                                expires_at TIMESTAMP NOT NULL,      -- LocalDateTime
                                revoked_at TIMESTAMP                -- null 허용 (로그아웃 시 시간 기록)
);
CREATE INDEX idx_refresh_tokens_hash ON refresh_tokens (token_hash);
CREATE INDEX idx_refresh_tokens_user_id ON refresh_tokens (user_id);