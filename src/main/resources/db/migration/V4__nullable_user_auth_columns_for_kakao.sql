-- 카카오 소셜 로그인 사용자는 localId(login_id)와 password가 없다.
-- LOCAL 전용 컬럼에 더미 값을 삽입하는 대신 nullable로 변경한다.
ALTER TABLE user_auth ALTER COLUMN login_id DROP NOT NULL;
ALTER TABLE user_auth ALTER COLUMN password DROP NOT NULL;

-- 기존 unique constraint (provider, login_id) 는 NULL을 NULL != NULL 로 처리하므로
-- 카카오 사용자가 여럿이어도 중복 위반이 발생하지 않는다.
-- LOCAL 사용자에 한해 중복을 막기 위해 partial unique index로 교체한다.
-- (인덱스 생성은 잠금 최소화를 위해 V5에서 CONCURRENTLY로 수행한다.)
ALTER TABLE user_auth DROP CONSTRAINT IF EXISTS uq_user_auth_provider_login_id;
