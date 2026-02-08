-- V1: Users table
CREATE TABLE users (
    id              BIGSERIAL PRIMARY KEY,
    email           VARCHAR(255) UNIQUE NOT NULL,
    password_hash   VARCHAR(255),
    provider        VARCHAR(20) DEFAULT 'LOCAL',
    provider_id     VARCHAR(255),
    display_name    VARCHAR(100),
    role            VARCHAR(20) DEFAULT 'USER',
    created_at      TIMESTAMPTZ DEFAULT NOW(),
    updated_at      TIMESTAMPTZ DEFAULT NOW()
);

CREATE INDEX idx_users_email ON users (email);
CREATE INDEX idx_users_provider ON users (provider, provider_id);
