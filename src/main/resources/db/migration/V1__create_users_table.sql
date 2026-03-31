CREATE TABLE users (
                       id UUID PRIMARY KEY,
                       email VARCHAR(255) UNIQUE NOT NULL,
                       password_hash VARCHAR(255) NOT NULL,
                       nickname VARCHAR(16) NOT NULL,
                       profile_img VARCHAR(255),
                       created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
                       deleted_at TIMESTAMPTZ NULL DEFAULT NULL
);