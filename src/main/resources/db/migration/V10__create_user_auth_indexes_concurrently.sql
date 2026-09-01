-- CREATE UNIQUE INDEX CONCURRENTLY 는 트랜잭션 안에서 실행할 수 없다.
-- Flyway가 이 파일을 트랜잭션 없이 실행하도록 아래 주석을 반드시 유지한다.
-- flyway:executeInTransaction=false

-- LOCAL 사용자에 한해 login_id 중복을 막는 partial unique index.
CREATE UNIQUE INDEX CONCURRENTLY IF NOT EXISTS uq_user_auth_local_login_id
    ON user_auth (login_id)
    WHERE provider = 'LOCAL';

-- 같은 소셜 계정(provider + provider_user_id)의 중복 행을 방지하는 partial unique index.
-- provider_user_id가 null인 LOCAL 사용자는 제외한다.
CREATE UNIQUE INDEX CONCURRENTLY IF NOT EXISTS uq_user_auth_provider_user_id
    ON user_auth (provider, provider_user_id)
    WHERE provider_user_id IS NOT NULL;
