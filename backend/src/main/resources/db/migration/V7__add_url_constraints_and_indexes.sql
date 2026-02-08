-- V7: Additional URL domain constraints and indexes for Week 2

-- Check constraint: long_url must be a valid URL prefix
ALTER TABLE urls ADD CONSTRAINT chk_long_url_format
    CHECK (long_url ~* '^https?://');

-- Check constraint: short_code must be alphanumeric (4–10 chars)
ALTER TABLE urls ADD CONSTRAINT chk_short_code_format
    CHECK (short_code ~ '^[a-zA-Z0-9\-_]{1,10}$');

-- Partial index for active, non-expired URLs (redirect hot path)
CREATE INDEX idx_urls_active_redirect ON urls (short_code)
    WHERE is_active = TRUE;

-- Index for expiry cleanup job
CREATE INDEX idx_urls_expiry_cleanup ON urls (expires_at)
    WHERE is_active = TRUE AND expires_at IS NOT NULL;

-- Index for user URL listing (covers the paginated query)
CREATE INDEX idx_urls_user_active_created ON urls (user_id, created_at DESC)
    WHERE is_active = TRUE;
