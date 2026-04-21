CREATE TABLE questions (
                           id          UUID        PRIMARY KEY DEFAULT gen_random_uuid(),
                           quiz_id     UUID        NOT NULL REFERENCES quizzes(id) ON DELETE CASCADE,
                           youtube_url VARCHAR(255) NOT NULL,
                           start_sec   INT         NOT NULL,
                           end_sec     INT         NOT NULL,
                           answers     TEXT[]      NOT NULL,
                           hint        VARCHAR(20),
                           order_index INT         NOT NULL,

                           CONSTRAINT questions_sec_range_check
                               CHECK (end_sec > start_sec AND (end_sec - start_sec) <= 30),
                           CONSTRAINT questions_start_sec_check
                               CHECK (start_sec >= 0),
                           CONSTRAINT questions_answers_length_check
                               CHECK (array_length(answers, 1) BETWEEN 1 AND 5),
                           CONSTRAINT questions_order_index_check
                               CHECK (order_index >= 1),
                           CONSTRAINT questions_quiz_order_unique
                               UNIQUE (quiz_id, order_index)
);

COMMENT ON TABLE  questions              IS '퀴즈에 속하는 문제. 퀴즈 삭제 시 CASCADE DELETE.';
COMMENT ON COLUMN questions.youtube_url  IS 'YouTube 영상 전체 URL. 등록 시 임베드 가능 여부를 서버에서 검증한다.';
COMMENT ON COLUMN questions.start_sec    IS '재생 시작 지점 (초). 0 이상.';
COMMENT ON COLUMN questions.end_sec      IS '재생 종료 지점 (초). start_sec보다 크고, 구간이 최대 30초.';
COMMENT ON COLUMN questions.answers      IS '정답 문자열 배열. 1개 이상 5개 이하. 게임 중 클라이언트에 미전달.';
COMMENT ON COLUMN questions.hint         IS '힌트 문자열. 선택 입력. 게임 중 question:start 이벤트에 포함.';
COMMENT ON COLUMN questions.order_index  IS '퀴즈 내 문제 순서. 1부터 시작. 같은 quiz_id 내 중복 불가.';