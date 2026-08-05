-- email 컬럼 추가 (nullable로 먼저 추가)
ALTER TABLE users ADD COLUMN IF NOT EXISTS email VARCHAR(255);

-- 기존 행 백필 (기존 데이터가 있을 경우 임시 값 설정)
UPDATE users SET email = 'unknown_' || user_id || '@placeholder.com' WHERE email IS NULL;

-- NOT NULL 제약 적용
ALTER TABLE users ALTER COLUMN email SET NOT NULL;

-- UNIQUE 제약 적용
ALTER TABLE users ADD CONSTRAINT uq_users_email UNIQUE (email);
