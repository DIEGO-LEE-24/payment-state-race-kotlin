-- 한 결제에 딸린 항목별 상태를 한 컬럼의 문자열로 들고 있는 구조.
-- 예: "A=PAID,B=CANCELLED". 상태를 바꾸려면 읽고-고치고-통째로 다시 쓴다.
CREATE TABLE IF NOT EXISTS payment (
    id        BIGINT PRIMARY KEY,
    item_states TEXT NOT NULL
);

-- 결제와 1:1인 세션 행. "처음 본 시각"을 한 번만 기록하려고 둔 것.
CREATE TABLE IF NOT EXISTS payment_session (
    payment_id BIGINT PRIMARY KEY,
    first_seen_at TIMESTAMPTZ
);
