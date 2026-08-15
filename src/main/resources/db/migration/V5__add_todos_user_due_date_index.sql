-- todos 목록/상세 조회는 user_id (+ 정렬용 due_date) 로 항상 필터링하는데
-- 지금까지 이 조회를 지원하는 인덱스가 없어 user_id 기준 풀스캔에 가깝게 걸린다.
-- 삭제된(deleted_at IS NOT NULL) 행은 목록 조회에서 항상 제외되므로
-- 인덱스도 그 범위로 한정해 크기를 줄인다.
CREATE INDEX IF NOT EXISTS idx_todos_user_due_date
    ON todos (user_id, due_date)
    WHERE deleted_at IS NULL;
