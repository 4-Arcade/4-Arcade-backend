CREATE TABLE quizzes (
                         id UUID PRIMARY KEY,
                         user_id UUID NOT NULL REFERENCES users(id),
                         title VARCHAR(50) NOT NULL,
                         category VARCHAR(20) NOT NULL,
                         is_public BOOLEAN NOT NULL DEFAULT TRUE,
                         play_count INT NOT NULL DEFAULT 0,
                         created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
                         updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),

                         CONSTRAINT quizzes_category_check
                             CHECK (category IN ('K-POP', 'POP', 'OST', '게임음악', '기타')),
                         CONSTRAINT quizzes_play_count_check
                             CHECK (play_count >= 0)
);