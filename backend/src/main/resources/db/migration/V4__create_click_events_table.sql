-- V4: Click Events (partitioned by month)
CREATE TABLE click_events (
    id              BIGSERIAL,
    event_id        UUID NOT NULL,
    url_id          BIGINT NOT NULL,
    short_code      VARCHAR(10) NOT NULL,
    clicked_at      TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    ip_address      INET,
    user_agent      TEXT,
    referrer        TEXT,
    device_type     VARCHAR(20),
    browser         VARCHAR(50),
    os              VARCHAR(50),
    country         VARCHAR(100),
    city            VARCHAR(100),
    latitude        DOUBLE PRECISION,
    longitude       DOUBLE PRECISION,
    PRIMARY KEY (id, clicked_at),
    UNIQUE (event_id, clicked_at)
) PARTITION BY RANGE (clicked_at);

-- Create initial monthly partition
CREATE TABLE click_events_2026_02 PARTITION OF click_events
    FOR VALUES FROM ('2026-02-01') TO ('2026-03-01');

CREATE TABLE click_events_2026_03 PARTITION OF click_events
    FOR VALUES FROM ('2026-03-01') TO ('2026-04-01');

CREATE INDEX idx_click_events_short_code ON click_events (short_code, clicked_at);
CREATE INDEX idx_click_events_url_id ON click_events (url_id, clicked_at);
