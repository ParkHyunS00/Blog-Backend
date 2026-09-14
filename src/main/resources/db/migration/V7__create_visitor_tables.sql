CREATE TABLE visitor_records (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    visitor_id VARCHAR(36) NOT NULL,
    visit_date DATE NOT NULL,
    created_at DATETIME(6) NOT NULL,
    updated_at DATETIME(6) NOT NULL,

    CONSTRAINT uk_visitor_records_date_visitor UNIQUE (visit_date, visitor_id)
);

CREATE TABLE visitor_daily_stats (
    visit_date DATE PRIMARY KEY,
    visitor_count BIGINT NOT NULL DEFAULT 0,
    created_at DATETIME(6) NOT NULL,
    updated_at DATETIME(6) NOT NULL,

    CONSTRAINT chk_visitor_daily_stats_count_not_negative CHECK (visitor_count >= 0)
);
