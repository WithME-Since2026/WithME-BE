-- categories: 사용자별 정렬 순서 유니크 제약
--
-- 애플리케이션은 사용자 행 비관 잠금으로 정렬 순서 재계산을 직렬화하지만,
-- 잠금은 모든 쓰기 경로가 성실히 지킬 때만 동작하는 규약이다.
-- 최종 방어선은 DB 에 둔다.
--
-- DEFERRABLE INITIALLY DEFERRED 인 이유:
-- 재정렬은 목록 전체의 sort_order 를 다시 매기므로 UPDATE 문 사이에
-- 두 행이 같은 값을 갖는 중간 상태가 반드시 생긴다.
-- 즉시 검사하는 제약이면 정상 재정렬조차 실패하므로 커밋 시점으로 미룬다.
--
-- @Table(uniqueConstraints) 로 표현할 수 없어(deferrable 미지원) Flyway 로 관리한다.
-- prod 의 ddl-auto=validate 는 유니크 제약을 검증하지 않으므로 충돌하지 않는다.

-- 제약을 걸기 전에, 잠금 도입 이전의 경합으로 생겼을 수 있는
-- 중복/비연속 순서를 사용자별로 0..n-1 로 정규화한다.
-- 기존 순서를 최대한 보존하고, 값이 같으면 category_id 로 안정적으로 결정한다.
WITH renumbered AS (
    SELECT category_id,
           ROW_NUMBER() OVER (
               PARTITION BY user_id
               ORDER BY sort_order, category_id
           ) - 1 AS new_sort_order
    FROM categories
)
UPDATE categories c
SET sort_order = r.new_sort_order
FROM renumbered r
WHERE c.category_id = r.category_id
  AND c.sort_order <> r.new_sort_order;

-- ADD CONSTRAINT 는 IF NOT EXISTS 를 지원하지 않으므로 카탈로그를 직접 확인한다.
DO $$
BEGIN
    IF NOT EXISTS (
        SELECT 1 FROM pg_constraint WHERE conname = 'uk_category_user_sort_order'
    ) THEN
        ALTER TABLE categories
            ADD CONSTRAINT uk_category_user_sort_order
            UNIQUE (user_id, sort_order)
            DEFERRABLE INITIALLY DEFERRED;
    END IF;
END $$;
