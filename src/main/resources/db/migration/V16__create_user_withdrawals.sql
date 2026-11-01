CREATE TABLE user_withdrawals
(
    withdrawal_id BIGSERIAL PRIMARY KEY,
    user_id BIGINT NOT NULL UNIQUE REFERENCES users (user_id),
    reason VARCHAR(100) NOT NULL,
    opinion VARCHAR(1000),
    created_at TIMESTAMP NOT NULL DEFAULT now(),
    updated_at TIMESTAMP NOT NULL DEFAULT now()
);
