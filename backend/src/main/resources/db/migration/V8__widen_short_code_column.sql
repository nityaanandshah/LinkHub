-- V8: Widen short_code column to support longer custom aliases (up to 30 chars)
ALTER TABLE urls ALTER COLUMN short_code TYPE VARCHAR(30);
