-- user_token: ACTIVE 상태 토큰에 대한 partial unique index
-- JPA @Index로는 표현 불가하므로 Flyway로 관리
CREATE UNIQUE INDEX IF NOT EXISTS uq_user_token_active
    ON user_token (user_id, provider)
    WHERE status = 'ACTIVE';
