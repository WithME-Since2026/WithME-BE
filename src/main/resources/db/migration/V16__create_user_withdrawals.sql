CREATE TABLE user_withdrawals
(
    withdrawal_id BIGSERIAL PRIMARY KEY,
    user_id BIGINT NOT NULL REFERENCES users (user_id),
    reason VARCHAR(100),
    opinion VARCHAR(1000),
    created_at TIMESTAMP NOT NULL DEFAULT now(),
    updated_at TIMESTAMP NOT NULL DEFAULT now()
);

CREATE INDEX idx_user_withdrawals_user_id ON user_withdrawals (user_id);
