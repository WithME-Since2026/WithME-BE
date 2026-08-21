CREATE TABLE recurrences (
    recurrence_id BIGSERIAL PRIMARY KEY,
    owner_type VARCHAR(20) NOT NULL,
    owner_id BIGINT NOT NULL,
    freq VARCHAR(10) NOT NULL,
    repeat_interval INTEGER NOT NULL DEFAULT 1,
    by_days VARCHAR(27),
    end_type VARCHAR(10) NOT NULL,
    end_date DATE,
    end_count INTEGER,
    CONSTRAINT uq_recurrences_owner UNIQUE (owner_type, owner_id),
    CONSTRAINT ck_recurrences_owner_type CHECK (owner_type IN ('TODO', 'SCHEDULE')),
    CONSTRAINT ck_recurrences_freq CHECK (freq IN ('DAILY', 'WEEKLY', 'MONTHLY')),
    CONSTRAINT ck_recurrences_interval CHECK (repeat_interval > 0),
    CONSTRAINT ck_recurrences_by_days CHECK (
        (freq = 'WEEKLY'
            AND by_days IS NOT NULL
            AND by_days ~ '^(MON|TUE|WED|THU|FRI|SAT|SUN)(,(MON|TUE|WED|THU|FRI|SAT|SUN))*$')
        OR (freq IN ('DAILY', 'MONTHLY') AND by_days IS NULL)
    ),
    CONSTRAINT ck_recurrences_end CHECK (
        (end_type = 'NEVER' AND end_date IS NULL AND end_count IS NULL)
        OR (end_type = 'DATE' AND end_date IS NOT NULL AND end_count IS NULL)
        OR (end_type = 'COUNT' AND end_date IS NULL AND end_count BETWEEN 1 AND 1000)
    )
);

CREATE TABLE recurrence_exceptions (
    exception_id BIGSERIAL PRIMARY KEY,
    recurrence_id BIGINT NOT NULL,
    occurrence_date DATE NOT NULL,
    exception_type VARCHAR(10) NOT NULL,
    override_date DATE,
    override_title VARCHAR(255),
    override_start_time TIME,
    override_end_time TIME,
    override_completed BOOLEAN,
    CONSTRAINT fk_recurrence_exceptions_recurrence
        FOREIGN KEY (recurrence_id) REFERENCES recurrences(recurrence_id) ON DELETE CASCADE,
    CONSTRAINT uq_recurrence_exceptions_occurrence UNIQUE (recurrence_id, occurrence_date),
    CONSTRAINT ck_recurrence_exceptions_type CHECK (exception_type IN ('SKIP', 'OVERRIDE')),
    CONSTRAINT ck_recurrence_exceptions_payload CHECK (
        (exception_type = 'SKIP'
            AND override_date IS NULL
            AND override_title IS NULL
            AND override_start_time IS NULL
            AND override_end_time IS NULL
            AND override_completed IS NULL)
        OR (exception_type = 'OVERRIDE'
            AND NOT (
                override_date IS NULL
                AND override_title IS NULL
                AND override_start_time IS NULL
                AND override_end_time IS NULL
                AND override_completed IS NULL
            ))
    )
);
