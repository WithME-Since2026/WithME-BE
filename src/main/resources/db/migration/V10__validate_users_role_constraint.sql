-- V8 에서 NOT VALID 로 붙인 제약의 기존 행 검증.
-- VALIDATE CONSTRAINT 는 SHARE UPDATE EXCLUSIVE 만 잡아 쓰기를 막지 않지만,
-- V8 과 같은 트랜잭션에 있으면 V8 의 잠금이 커밋까지 유지되므로 마이그레이션을 분리한다.
--
-- 이미 검증된 제약에 다시 실행해도 안전하지만(무시된다),
-- 제약이 없는 DB 에서 실패하지 않도록 존재 여부를 확인한다.
DO $$
BEGIN
    IF EXISTS (
        SELECT 1 FROM pg_constraint
        WHERE conrelid = 'users'::regclass
          AND conname = 'ck_users_role'
    ) THEN
        ALTER TABLE users VALIDATE CONSTRAINT ck_users_role;
    END IF;
END $$;
