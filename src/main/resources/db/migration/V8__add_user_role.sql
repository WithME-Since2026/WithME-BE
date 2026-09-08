ALTER TABLE users
    ADD COLUMN role VARCHAR(20) NOT NULL DEFAULT 'USER';

-- NOT VALID 로 붙여 기존 행 전수 검사를 건너뛴다.
-- 즉시 검사하면 users 전체를 스캔하는 동안 잠금이 마이그레이션 커밋까지 유지된다.
-- 기존 행 검증은 V10 에서 별도 트랜잭션으로 수행한다.
ALTER TABLE users
    ADD CONSTRAINT ck_users_role CHECK (role IN ('USER', 'ADMIN')) NOT VALID;
