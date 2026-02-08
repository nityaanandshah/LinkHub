-- V2: Pre-generated Key Pool
CREATE TABLE key_pool (
    id              BIGSERIAL PRIMARY KEY,
    short_key       VARCHAR(7) UNIQUE NOT NULL,
    is_used         BOOLEAN DEFAULT FALSE,
    claimed_at      TIMESTAMPTZ
);

CREATE INDEX idx_key_pool_unused ON key_pool (id) WHERE is_used = FALSE;
