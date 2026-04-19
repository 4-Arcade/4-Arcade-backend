-- 문제 수를 저장할 컬럼 추가
ALTER TABLE quizzes
    ADD COLUMN question_count INT NOT NULL DEFAULT 0;

-- 문제 수에 대한 제약 조건(음수 방지) 및 코멘트 추가
ALTER TABLE quizzes
    ADD CONSTRAINT quizzes_question_count_check CHECK (question_count >= 0);

COMMENT ON COLUMN quizzes.question_count IS '퀴즈에 포함된 총 문제 수. 방 생성 시 최소 문제 수(5개) 검증에 사용.';