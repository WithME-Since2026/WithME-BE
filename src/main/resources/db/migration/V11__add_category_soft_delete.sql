-- categories: 소프트 딜리트 도입
--
-- deleted_at 컬럼을 추가한다.
--
-- uk_category_user_name: 삭제된(deleted_at IS NOT NULL) 카테고리가 이름을 계속 점유하면
-- 같은 이름으로 다시 만들 수 없게 되므로, deleted_at IS NULL 인 행에만 적용되는
-- 부분(partial) 유니크 인덱스로 전환한다.
-- Postgres 는 테이블 제약(ADD CONSTRAINT ... UNIQUE)에 WHERE 절을 허용하지 않으므로
-- CREATE UNIQUE INDEX 로 관리한다. 위반 시 에러 메시지에 인덱스 이름이 그대로 실려서
-- ConstraintViolations.matches() 의 이름 기반 판별은 그대로 동작한다.
--
-- uk_category_user_sort_order(V2)는 재정렬 중간 상태를 허용하기 위해 DEFERRABLE 이어야 하는데,
-- Postgres 에서는 부분(partial) 인덱스를 DEFERRABLE 로 만들 수 없다.
-- 그래서 이 제약은 그대로 전체 테이블에 유지하고, 대신 삭제 시 Category.delete() 가
-- sort_order 를 -category_id(항상 음수, 카테고리별 유일)로 밀어내
-- 활성 목록의 재정렬(0..n-1)과 절대 충돌하지 않도록 한다.

ALTER TABLE categories ADD COLUMN IF NOT EXISTS deleted_at TIMESTAMP;

DO $$
BEGIN
    IF EXISTS (
        SELECT 1 FROM pg_constraint
        WHERE conrelid = 'categories'::regclass
          AND conname = 'uk_category_user_name'
    ) THEN
        ALTER TABLE categories DROP CONSTRAINT uk_category_user_name;
    END IF;
END $$;

CREATE UNIQUE INDEX IF NOT EXISTS uk_category_user_name
    ON categories (user_id, category_name)
    WHERE deleted_at IS NULL;
