CREATE TABLE schedules (
    schedule_id BIGSERIAL PRIMARY KEY,
    user_id BIGINT NOT NULL REFERENCES users(user_id),
    title VARCHAR(255) NOT NULL,
    all_day BOOLEAN NOT NULL DEFAULT FALSE,
    start_date DATE NOT NULL,
    start_time TIME NULL,
    end_date DATE NOT NULL,
    end_time TIME NULL,
    deleted_at TIMESTAMP NULL,
    created_at TIMESTAMP NOT NULL,
    updated_at TIMESTAMP NOT NULL,
    CONSTRAINT ck_schedules_date_order CHECK (end_date >= start_date),
    CONSTRAINT ck_schedules_time_presence CHECK (
        (all_day AND start_time IS NULL AND end_time IS NULL)
        OR (NOT all_day AND start_time IS NOT NULL AND end_time IS NOT NULL)
    ),
    CONSTRAINT ck_schedules_time_order CHECK (
        start_date <> end_date OR all_day OR end_time >= start_time
    )
);

CREATE INDEX idx_schedules_user_start_date
    ON schedules (user_id, start_date);
