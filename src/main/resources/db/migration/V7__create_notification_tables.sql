CREATE TABLE notifications
(
    notification_id BIGSERIAL PRIMARY KEY,
    user_id         BIGINT       NOT NULL REFERENCES users (user_id),
    type            VARCHAR(30)  NOT NULL,
    title           VARCHAR(100) NOT NULL,
    body            VARCHAR(255) NOT NULL,
    read_at         TIMESTAMP,
    created_at      TIMESTAMP    NOT NULL DEFAULT now(),
    updated_at      TIMESTAMP    NOT NULL DEFAULT now()
);

CREATE INDEX idx_notifications_user_id ON notifications (user_id);
CREATE INDEX idx_notifications_user_unread ON notifications (user_id) WHERE read_at IS NULL;

CREATE TABLE fcm_tokens
(
    token_id   BIGSERIAL PRIMARY KEY,
    user_id    BIGINT       NOT NULL REFERENCES users (user_id),
    device_id  VARCHAR(128) NOT NULL,
    token      VARCHAR(512) NOT NULL,
    created_at TIMESTAMP    NOT NULL DEFAULT now(),
    updated_at TIMESTAMP    NOT NULL DEFAULT now(),
    CONSTRAINT uq_fcm_tokens_user_device UNIQUE (user_id, device_id)
);
