CREATE TABLE holidays (
    holiday_id BIGSERIAL PRIMARY KEY,
    date DATE NOT NULL,
    name VARCHAR(50) NOT NULL,
    type VARCHAR(20) NOT NULL,
    rest_day BOOLEAN NOT NULL,
    CONSTRAINT uq_holidays_date_name UNIQUE (date, name),
    CONSTRAINT ck_holidays_type CHECK (
        type IN ('HOLIDAY', 'NATIONAL', 'ANNIVERSARY', 'SOLAR_TERM', 'SUNDRY')
    )
);

CREATE INDEX idx_holidays_date ON holidays (date);
