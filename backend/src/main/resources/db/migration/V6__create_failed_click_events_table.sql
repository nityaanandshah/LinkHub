-- V6: Dead Letter Queue for failed Kafka events
CREATE TABLE failed_click_events (
    id              BIGSERIAL PRIMARY KEY,
    event_id        UUID NOT NULL,
    payload         JSONB NOT NULL,
    failure_reason  TEXT,
    retry_count     INT DEFAULT 0,
    created_at      TIMESTAMPTZ DEFAULT NOW(),
    next_retry_at   TIMESTAMPTZ
);

CREATE INDEX idx_failed_events_retry ON failed_click_events (next_retry_at)
    WHERE retry_count < 5;
